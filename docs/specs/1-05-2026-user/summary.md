# Feature: User — Implementation Summary

## What shipped

User management backed by Firebase Auth. Every authenticated `/api/**` request is intercepted by `FirebaseAuthFilter`, which verifies the Bearer token and either finds or creates the corresponding MedSync user. The admin endpoint lets a COO create users with a specific role; rollback compensates if MedSync DB save fails after a Firebase user was created.

## Endpoints delivered

| Method | Path                  | Status codes                  |
|--------|-----------------------|-------------------------------|
| GET    | `/api/users/me`       | 200 / 401                     |
| POST   | `/api/auth/login`     | 200 / 401 / 403               |
| POST   | `/api/admin/users`    | 201 / 400 / 401 / 403 / 409   |

## Migrations applied

- `V2__create_users_schema.sql`
- `V3__seed_roles.sql` (COO / DOCTOR / CMO with deterministic UUIDs)
- `V4__add_patient_medico_fk.sql` (back-fills FK deferred from the patient feature)

## Deviations from design

- The `activo` column was added to the schema for future soft-delete but no service uses it today.

## Deferred from scope

- Editing users (`PUT` / `PATCH`).
- Deactivation endpoint (`activo` flag).
- Password reset (Firebase handles it client-side).
- Role-mutation endpoint.
- `@RolesAllowed` integration.

## Acknowledged technical debt

1. **Manual role checks** scattered in resources. Acceptable today; refactor to `@RolesAllowed` if endpoint count grows.
2. **Compensation logic in `CreateAdminUserService` is a hand-rolled saga.** Edge cases (Firebase delete failure after DB save failure) currently log and surface as 500. Acceptable but worth revisiting.
3. **`AuthenticatedUserContext`** is a hidden dependency for services that bypass the filter (background jobs). The PubMed scheduler does this — see `article` spec.

## Tests delivered

| File                              | Type                  | What it covers                                      |
|-----------------------------------|-----------------------|-----------------------------------------------------|
| `UserTest`, `RoleTest`            | unit                  | Invariants, factories, equals/hashCode              |
| `RegisterUserServiceTest`         | unit (Mockito)        | New vs. existing user                               |
| `CreateAdminUserServiceTest`      | unit (Mockito)        | Happy path, duplicate, role-not-found, rollback     |
| `UserRepositoryImplTest`          | `@QuarkusTest`        | EntityGraph eager-load of role                      |
| `*ResourceIT`                     | RestAssured           | Status codes, role gating, validation               |

## Features that depend on this one

- **All authenticated features** rely on `FirebaseAuthFilter` + `AuthenticatedUserContext`.
- **`patient`** — `medico_id` FK now constrained against `usuarios.id` via V4.
- **`medication`** — every endpoint requires `AuthenticatedUserContext` (manual check).
- **`patient-context`**, **`article`** — same.
- **`alert`** (planned) — alerts addressed to specific doctors require user resolution.
