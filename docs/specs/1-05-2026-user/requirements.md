# Feature: User — Requirements

## Overview

User management and authentication for MedSync. Users have roles (`COO`, `DOCTOR`, `CMO`) that gate access to features. Authentication delegates to **Firebase Auth** — MedSync does not store passwords, only the user's identity, role, and metadata. The default role for any new login is `DOCTOR`; only a `COO` may create users with a different role.

## User stories

- As a **first-time visitor**, I want to sign in with my email through the frontend's Firebase login, so that MedSync auto-creates my account with the `DOCTOR` role.
- As an **existing user**, I want to sign in, so that the system loads my profile and role and returns me to the app.
- As an **existing user**, I want to retrieve my own profile, so that the frontend can display my name and role.
- As a **COO**, I want to create another user (with a chosen role), so that I can onboard new doctors or admin staff.
- As any **authenticated user**, I want my role to be enforced on protected endpoints, so that I cannot access functionality outside my scope.

## Acceptance criteria (EARS format)

### Ubiquitous
- The system shall use Firebase Auth as the source of truth for authentication. Passwords are never stored in MedSync's database.
- The system shall maintain a local mirror of users (`usuarios` table) keyed by Firebase email, storing the assigned role.
- The system shall seed three roles (`COO`, `DOCTOR`, `CMO`) with deterministic UUIDs at first migration.

### Event-driven
- When a request reaches any `/api/**` endpoint with a valid Firebase Bearer token, the system shall:
  - Resolve the Firebase identity (email, display name).
  - Find or create the corresponding MedSync user (default role `DOCTOR` on creation).
  - Populate a request-scoped `AuthenticatedUserContext` for downstream code.
- When a login request arrives at `POST /api/auth/login` for a user whose role matches the `expectedRole` query, the system shall return `200 OK` with the user profile.
- When a `GET /api/users/me` request arrives from an authenticated user, the system shall return `200 OK` with the user profile.
- When a `POST /api/admin/users` request arrives from a COO with a valid payload, the system shall:
  - Verify the email is not already used in MedSync or Firebase.
  - Create the user in Firebase Auth.
  - Persist the user locally.
  - On local persistence failure, the system shall **delete the Firebase user** (compensation step).
  - Return `201 Created` with the user profile.

### Conditional
- If the Bearer token is missing or invalid, the system shall return `401 Unauthorized`.
- If the caller of `POST /api/admin/users` is not a COO, the system shall return `403 Forbidden`.
- If the login's `expectedRole` does not match the user's actual role, the system shall return `403` (`RoleMismatchException`) including both roles in the message.
- If the email already exists in either Firebase or MedSync DB during user creation, the system shall return `409 Conflict` (`UserAlreadyExistsException`).
- If the requested role does not exist, the system shall return `404` (`RoleNotFoundException`).
- If the create-user payload fails validation, the system shall return `400`.

## Non-functional requirements

- **No password storage**: passwords live in Firebase only.
- **Identity portability**: Firebase identities are mapped 1-to-1 with MedSync users by email.
- **Compensation on failure**: if MedSync persistence fails after a Firebase user is created, the Firebase user must be deleted to keep the systems consistent.
- **Configurability**: Firebase service-account credentials path comes from `firebase.config.path`.

## Out of scope (explicit)

- Editing users (`PUT` / `PATCH`).
- Soft-delete / deactivation (the `activo` column exists but no use case uses it yet).
- Password reset (Firebase handles it client-side).
- Role-mutation endpoint.
- Role-based access via `@RolesAllowed` (we enforce manually per endpoint).

## Open questions

- Should we use `@RolesAllowed` once we have more endpoints to protect, instead of manual checks? *Not blocking — revisit when feature count grows.*
- Activate the `activo` flag with a deactivation endpoint? *Not blocking — no current need.*
