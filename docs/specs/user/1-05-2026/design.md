# Feature: User — Design

## Domain models

### `User` (immutable, `domain/user/model/User.java`)

| Field        | Type            | Required | Notes                              |
|--------------|-----------------|----------|------------------------------------|
| id           | `UUID`          | yes      | generated server-side              |
| nombre       | `String`        | yes      |                                    |
| correo       | `String`        | yes      | must contain `@`, not "invalid@" placeholder |
| rolId        | `UUID`          | yes      | references `Role.id`               |
| activo       | `boolean`       | yes      | currently always `true`            |
| createdAt    | `LocalDateTime` | auto     |                                    |

Invariants in constructor: all not null; `nombre`, `correo` not blank; `correo` passes basic email sanity check.
Factory: `User.create(...)` — generates UUID and uses default role.
Behavior: `User.deactivate()` returns a copy with `activo = false`.

### `Role` (immutable)

| Field        | Type            | Required |
|--------------|-----------------|----------|
| id           | `UUID`          | yes      |
| nombre       | `String`        | yes      |
| descripcion  | `String`        | no       |
| createdAt    | `LocalDateTime` | auto     |

Invariants: `id`, `nombre` not null nor blank.

### `UserWithRole` (record, transport)

```java
public record UserWithRole(User user, String roleName) {}
```

Used to ship a user + role name across layers without repeated lookups.

### `KnownRoles` (constants)

```java
public final class KnownRoles {
    public static final String COO    = "COO";
    public static final String DOCTOR = "DOCTOR";
    public static final String CMO    = "CMO";
}
```

## Output ports

```java
public interface UserRepository {
    Optional<UserWithRole> findByEmail(String email);
    User save(User user);
}

public interface RoleRepository {
    Optional<Role> findByNombre(String nombre);
    Optional<Role> findByUuid(UUID id);
}

public interface FirebaseUserGateway {
    String createFirebaseUser(String email, String password, String displayName);
    void deleteFirebaseUser(String firebaseUid);
}
```

`FirebaseUserGateway` is a `Gateway` (external system) per [ADR 0002](../../architecture/decisions/0002-gateway-vs-repository.md).

## Use cases

| Use case                          | Service                       | Trigger                                  |
|-----------------------------------|-------------------------------|------------------------------------------|
| `LoginOrRegisterUserUseCase`      | `RegisterUserService`         | every authenticated request (via filter) |
| `CreateAdminUserUseCase`          | `CreateAdminUserService`      | `POST /api/admin/users` from a COO       |

## REST endpoints

| Method | Path                  | Request                      | Response         | Status codes              |
|--------|-----------------------|------------------------------|------------------|---------------------------|
| GET    | `/api/users/me`       | —                            | `UserResponse`   | 200 / 401                 |
| POST   | `/api/auth/login`     | `LoginRequest` (optional `expectedRole`) | `UserResponse` | 200 / 401 / 403         |
| POST   | `/api/admin/users`    | `CreateUserRequest`          | `UserResponse`   | 201 / 400 / 401 / 403 / 409 |

Swagger tags: `Users`, `Auth`, `Admin`.

### DTOs (`interfaces/rest/{user,auth,admin}/`)

- `LoginRequest` — `expectedRole: String` (`@Size`, `@Pattern("^[A-Za-z_]{1,30}$")`, optional; default `DOCTOR`).
- `CreateUserRequest` — `correo` (`@Email`, `@Size(max=100)`), `nombre` (`@Size(max=100)`), `password` (`@Size(min=6, max=100)`), `rol` (`@Size(max=30)`).
- `UserResponse` — record: `id`, `nombre`, `correo`, `role`, `activo`, `createdAt`.

## Authentication flow

```
Client                FirebaseAuthFilter          Firebase           LoginOrRegister      UserRepo         Resource
  │  Bearer <token>   │                            │                  │                    │                │
  │ ─────────────────►│                            │                  │                    │                │
  │                   │ verifyIdToken(token) ─────►│                  │                    │                │
  │                   │ ◄── decoded (email, name) ─│                  │                    │                │
  │                   │ execute(email, name) ─────────────────────────►                    │                │
  │                   │                            │                  │ findByEmail(email) │                │
  │                   │                            │                  │ ─────────────────► │                │
  │                   │                            │                  │ if absent:         │                │
  │                   │                            │                  │   user ← new User(default role)     │
  │                   │                            │                  │   save(user)       │                │
  │                   │ AuthenticatedUserContext.set(userWithRole)    │                    │                │
  │                   │ forward request ─────────────────────────────────────────────────────────────────►│
  │                   │                            │                  │                    │       enforce role
  │                   │                            │                  │                    │       handle req
  │ ◄────────────── response ─────────────────────────────────────────────────────────────────────────────│
```

`FirebaseAuthFilter` is a `@Provider` ContainerRequestFilter at `Priorities.AUTHENTICATION`. It runs for every `/api/**` path. `AuthenticatedUserContext` is `@RequestScoped`.

## Persistence

### `UserEntity` (table `usuarios`)

| Column      | Type           | Constraints                                  |
|-------------|----------------|----------------------------------------------|
| id          | `BINARY(16)`   | PK                                           |
| nombre      | `VARCHAR(100)` | NOT NULL                                     |
| correo      | `VARCHAR(100)` | NOT NULL, UNIQUE                             |
| rol_id      | `BINARY(16)`   | NOT NULL, FK → `roles(id)`                   |
| activo      | `BOOLEAN`      | NOT NULL DEFAULT TRUE                        |
| created_at  | `TIMESTAMP`    | NOT NULL DEFAULT CURRENT_TIMESTAMP           |

Relation: `@ManyToOne(fetch = LAZY)` to `RoleEntity` via `rol_id` (`insertable=false, updatable=false`).
`@NamedEntityGraph("User.withRole")` eagerly fetches the role.

### `RoleEntity` (table `roles`)

| Column        | Type           | Constraints       |
|---------------|----------------|-------------------|
| id            | `BINARY(16)`   | PK                |
| nombre        | `VARCHAR(50)`  | NOT NULL, UNIQUE  |
| descripcion   | `TEXT`         |                   |
| created_at    | `TIMESTAMP`    | auto              |

### Migrations

- `V2__create_users_schema.sql` — creates `usuarios` and `roles` with FK.
- `V3__seed_roles.sql` — inserts COO, DOCTOR, CMO with deterministic UUIDs:
  - COO: `0x00000000000010008000000000000001`
  - DOCTOR: `0x00000000000010008000000000000002`
  - CMO: `0x00000000000010008000000000000003`
- `V4__add_patient_medico_fk.sql` — back-fills the FK from `patients.medico_id` → `usuarios.id` (deferred from V1 because `usuarios` didn't yet exist).

## Security / Firebase integration

- `FirebaseInitializer` (`@ApplicationScoped`, `@Startup`) loads service-account credentials from `firebase.config.path` and initializes `FirebaseApp`.
- `FirebaseAuthFilter` verifies the Bearer token, extracts identity, calls `LoginOrRegisterUserUseCase`, populates context.
- `FirebaseUserGatewayImpl` creates / deletes Firebase users. Throws `UserAlreadyExistsException` on duplicate email in Firebase.

## Validation

| Level     | Check                                                            | Location                          |
|-----------|------------------------------------------------------------------|-----------------------------------|
| Format    | `@Email`, `@Size(max=100)` on `correo`                            | `CreateUserRequest`               |
| Format    | `@Size(min=6, max=100)` on `password`                             | `CreateUserRequest`               |
| Format    | `@Pattern("^[A-Za-z_]{1,30}$")` on `expectedRole`                 | `LoginRequest`                    |
| Business  | `correo` contains `@`, not placeholder values                     | `User` constructor                |
| Business  | `nombre`, `correo` not blank; ids not null                        | `User` / `Role` constructors      |
| App-level | Role exists, email not duplicated, COO authorization              | `CreateAdminUserService`          |

## Exceptions

| Domain exception                | HTTP |
|---------------------------------|------|
| `UserNotFoundException`         | 404  |
| `UserAlreadyExistsException`    | 409  |
| `RoleNotFoundException`         | 404  |
| `RoleMismatchException`         | 403  |
| `InvalidUserDataException`      | 400  |
| `InvalidRoleDataException`      | 400  |

## Key technical decisions

### 1. Firebase Auth as identity provider

We do not store passwords. Firebase handles password hashing, reset flows, MFA, and abuse detection. MedSync's `usuarios` table mirrors Firebase identities (by email) so we can store roles and run our own authorization queries without round-trips to Firebase.

### 2. Default role `DOCTOR` for first-time logins

Any user authenticated via Firebase who is not yet in `usuarios` gets a row inserted with role `DOCTOR`. This is the lowest-privilege role. Elevated roles are assigned only via the admin endpoint by a COO.

### 3. Manual role checks (not `@RolesAllowed`)

Endpoints check `caller.roleName().equalsIgnoreCase("COO")` directly. We chose manual checks for clarity (the rule is visible at the call site) and to avoid wiring SecurityContext integration with Firebase. Once endpoints multiply, we may switch to `@RolesAllowed`.

### 4. Compensation on creation failure

`CreateAdminUserService` creates the Firebase user **first**, then persists in MedSync DB. If DB save fails, it deletes the Firebase user (manual saga). This is the simplest "transactional" pattern across the two systems.

### 5. Deterministic UUIDs for seeded roles

The three seed roles use fixed UUIDs so we can reference them by id in test fixtures and across environments without lookups.

### 6. `activo` column kept but unused

Soft-delete is prepared at the schema and domain level (`User.deactivate()` exists) but no service uses it. Reserved for the eventual deactivation endpoint.

### 7. `@RequestScoped AuthenticatedUserContext`

The filter sets it once per request; downstream code (resources, services) reads it without explicit passing. Trade-off: hidden dependency. Mitigated by the rule that only resources read it — services receive explicit arguments.
