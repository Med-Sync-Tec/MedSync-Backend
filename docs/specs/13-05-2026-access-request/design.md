# Feature: Solicitud Acceso — Design

## Domain model

### `SolicitudAcceso` — `domain/solicitud/model/SolicitudAcceso.java`

| Field        | Type            | Required | Notes                                                        |
|--------------|-----------------|----------|--------------------------------------------------------------|
| id           | `UUID`          | yes      | server-generated                                             |
| nombre       | `String`        | yes      |                                                              |
| correo       | `String`        | yes      | must be unique among PENDIENTE/RECHAZADO records             |
| rol          | `String`        | yes      | `DOCTOR` or `COO`                                            |
| estado       | `EstadoSolicitud` | yes    | `PENDIENTE` → `APROBADO` \| `RECHAZADO`                      |
| token        | `String`        | yes      | UUID string; used in approve/reject URLs; unique in DB       |
| createdAt    | `LocalDateTime` | auto     | set by factory method                                        |
| rejectedAt   | `LocalDateTime` | no       | populated by `rechazar()`; null until rejected               |

**No `password` field** — passwords are never stored.

### Behavior
- `SolicitudAcceso.create(nombre, correo, rol)` — factory; generates id + token; sets
  state to `PENDIENTE`.
- `solicitud.aprobar()` — mutates state to `APROBADO`.
- `solicitud.rechazar()` — mutates state to `RECHAZADO` and sets `rejectedAt = now()`.

### `EstadoSolicitud` — `domain/solicitud/model/EstadoSolicitud.java`

```java
public enum EstadoSolicitud { PENDIENTE, APROBADO, RECHAZADO }
```

### `AprobarSolicitudResult` — `domain/solicitud/model/AprobarSolicitudResult.java`

```java
public record AprobarSolicitudResult(UserWithRole user, String passwordResetLink) {}
```

Returned by `AprobarSolicitudUseCase.execute()` so the REST layer can include the link
in the approval email without coupling the application layer to the mailer.

## Output ports

### `SolicitudAccesoRepository` — `domain/solicitud/repository/`

```java
public interface SolicitudAccesoRepository {
    SolicitudAcceso save(SolicitudAcceso solicitud);
    Optional<SolicitudAcceso> findByToken(String token);
    // Returns the most recent PENDIENTE or RECHAZADO record for the given email.
    Optional<SolicitudAcceso> findByCorreo(String correo);
    void delete(SolicitudAcceso solicitud);
}
```

### `FirebaseUserGateway` — `domain/user/repository/` (extended)

```java
public interface FirebaseUserGateway {
    String createUser(String email, String password, String displayName);
    void deleteUser(String uid);
    String generatePasswordResetLink(String email); // NEW
}
```

## Use cases

| Interface                    | Service                        | Notes                                              |
|------------------------------|--------------------------------|----------------------------------------------------|
| `CrearSolicitudUseCase`      | `CrearSolicitudService`        | Validates duplicates + cooldown; persists          |
| `AprobarSolicitudUseCase`    | `AprobarSolicitudService`      | Creates Firebase user + DB user; returns reset link |
| `RechazarSolicitudUseCase`   | `RechazarSolicitudService`     | Marks RECHAZADO; returns solicitud (for email)     |

`CreateAdminUserUseCase` (existing, modified):
- Signature changed: `execute(email, nombre, roleName)` — password removed.
- Generates a 24-char `SecureRandom` password internally; never exposed outside the method.

## REST endpoints

| Method | Path                                   | Auth     | Request DTO         | Response        | Status codes        |
|--------|----------------------------------------|----------|---------------------|-----------------|---------------------|
| POST   | `/api/auth/register`                   | public   | `RegisterRequest`   | —               | 202 / 400 / 409 / 429 |
| GET    | `/api/solicitudes/{token}/aprobar`     | public   | —                   | HTML page       | 200 / 400 / 404     |
| GET    | `/api/solicitudes/{token}/rechazar`    | public   | —                   | HTML page       | 200 / 400 / 404     |

Both `/api/solicitudes/` paths are whitelisted in `FirebaseAuthFilter.PUBLIC_PATH_PREFIXES`.

### `RegisterRequest` DTO

```java
public class RegisterRequest {
    @NotBlank @Size(max = 100)                  public String nombre;
    @NotBlank @Email @Size(max = 100)           public String correo;
    @NotBlank @Pattern(regexp = "DOCTOR|COO")   public String rol;
    // No password field.
}
```

## Persistence

### Entity: `SolicitudAccesoEntity` — `infrastructure/persistence/solicitud/`

| Column       | Type           | Constraints                          |
|--------------|----------------|--------------------------------------|
| id           | `BINARY(16)`   | PK                                   |
| nombre       | `VARCHAR(100)` | NOT NULL                             |
| correo       | `VARCHAR(100)` | NOT NULL (no longer UNIQUE — same email can appear across RECHAZADO + new PENDIENTE) |
| rol          | `VARCHAR(30)`  | NOT NULL                             |
| estado       | `VARCHAR(20)`  | NOT NULL DEFAULT 'PENDIENTE'         |
| token        | `VARCHAR(36)`  | NOT NULL UNIQUE                      |
| created_at   | `TIMESTAMP`    | DEFAULT CURRENT_TIMESTAMP            |
| rejected_at  | `TIMESTAMP`    | NULL                                 |

### Migrations

- **`V10__create_solicitudes_acceso.sql`** — initial table with `password` and UNIQUE on `correo`.
- **`V11__solicitudes_acceso_security.sql`** — DROP COLUMN `password`, DROP INDEX `correo`, ADD COLUMN `rejected_at`.

### `findByCorreo` query

```java
find("correo = ?1 AND estado IN ('PENDIENTE', 'RECHAZADO') ORDER BY createdAt DESC", correo)
    .firstResultOptional()
```

Returns the most recent active (non-APROBADO) record for an email, so the service can
evaluate pending/cooldown state.

## Validation

| Level     | Check                                              | Location                            |
|-----------|----------------------------------------------------|-------------------------------------|
| Format    | `@NotBlank`, `@Email`, `@Size` on all fields       | `RegisterRequest`                   |
| Format    | `@Pattern(regexp = "DOCTOR\|COO")` on `rol`        | `RegisterRequest`                   |
| App-level | Email not already in `usuarios`                    | `CrearSolicitudService`             |
| App-level | No existing `PENDIENTE` for this email             | `CrearSolicitudService` → 409       |
| App-level | No `RECHAZADO` within cooldown window              | `CrearSolicitudService` → 429       |
| App-level | Solicitud must be `PENDIENTE` to approve/reject    | `AprobarSolicitudService`, `RechazarSolicitudService` → 400 |

## Exceptions

| Domain exception                              | HTTP |
|-----------------------------------------------|------|
| `SolicitudNotFoundException`                  | 404  |
| `SolicitudPendienteException`                 | 409  |
| `SolicitudRechazadaRecientementeException`    | 429  |
| `UserAlreadyExistsException` (existing)       | 409  |

All wired in `GlobalExceptionHandler`.

## Email flows

| Trigger         | To        | Subject                                        | Key content                        |
|-----------------|-----------|------------------------------------------------|------------------------------------|
| Register submit | Admin     | "Nueva solicitud de acceso — MedSync"          | Nombre, correo, rol + Approve/Reject buttons |
| Register submit | Applicant | "Solicitud de acceso recibida — MedSync"       | Confirmation; admin will review    |
| Approve         | Applicant | "Tu acceso a MedSync ha sido aprobado"         | Role badge + password-reset link   |
| Reject          | Applicant | "Tu solicitud de acceso a MedSync fue rechazada" | Contact admin if error            |

## Key technical decisions

### 1. Passwords are never stored
`CreateAdminUserService` generates a `SecureRandom` 24-char password internally and
passes it directly to `firebaseUserGateway.createUser(...)`. The password variable is
local and garbage-collected. The applicant sets their own password via the Firebase
reset link delivered in the approval email.

### 2. `RECHAZADO` records are retained (not deleted)
Keeping rejected records enables the cooldown check. The `findByCorreo` query retrieves
only `PENDIENTE` and `RECHAZADO` states; `APROBADO` records are ignored because the
email will already be in `usuarios`. Once the cooldown expires, `CrearSolicitudService`
deletes the old `RECHAZADO` record before creating a new `PENDIENTE`.

### 3. UNIQUE constraint on `correo` is dropped (V11)
A single email may appear as `RECHAZADO` and later as a new `PENDIENTE` after the
cooldown. The uniqueness invariant is enforced in application logic (no two `PENDIENTE`
for the same email) rather than at the DB layer.

### 4. Token single-use enforced via state check
Both `AprobarSolicitudService` and `RechazarSolicitudService` verify that
`estado == PENDIENTE` before processing. A second click returns `400 Bad Request`.
No separate "token used" flag is needed.

### 5. `AprobarSolicitudResult` record bridges application → REST layer
`AprobarSolicitudService` returns `(UserWithRole, passwordResetLink)`. This keeps the
mailer dependency in `interfaces/` (the REST layer) and away from `application/`, which
must not import `io.quarkus.mailer`.

### 6. Cooldown is configurable without redeploy
`medsync.solicitudes.rejection-cooldown-days` defaults to 5. `CrearSolicitudService`
reads it via `@ConfigProperty`. The value can be overridden per environment profile.

## Sequence: Register (submit)

```
Client         AuthResource        CrearSolicitudService    Repository       Mailer
  │  POST /register  │                    │                    │               │
  │ ──────────────► │ @Valid              │                    │               │
  │                 │ execute(n,c,r) ──► │                    │               │
  │                 │                    │ findByCorreo(email) │               │
  │                 │                    │ ──────────────────► │               │
  │                 │                    │ userRepo.findByEmail │              │
  │                 │                    │ ──────────────────► │               │
  │                 │                    │ SolicitudAcceso.create(...)         │
  │                 │                    │ save(solicitud) ──► │               │
  │                 │ ◄─ solicitud ──── │                    │               │
  │                 │ send admin email ─────────────────────────────────────► │
  │                 │ send confirm email ────────────────────────────────────► │
  │ ◄─ 202 ─────── │                    │                    │               │
```

## Sequence: Approve

```
Admin        SolicitudResource    AprobarSolicitudService   Firebase   Repository
  │  GET /{token}/aprobar │              │                     │           │
  │ ──────────────────── │              │                     │           │
  │                       │ execute(token) ─────────────────► │           │
  │                       │              │ findByToken ──────────────────► │
  │                       │              │ check PENDIENTE     │           │
  │                       │              │ createAdminUser(email, nombre, rol)
  │                       │              │ ─── SecureRandom pwd ──────────►│
  │                       │              │ generatePasswordResetLink ──────►│
  │                       │              │ solicitud.aprobar()              │
  │                       │              │ save(solicitud) ────────────────►│
  │                       │ ◄─ result ─ │                     │           │
  │                       │ send approval email (with reset link)          │
  │ ◄── 200 HTML page ── │              │                     │           │
```
