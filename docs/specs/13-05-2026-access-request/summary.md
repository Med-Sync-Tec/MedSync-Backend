# Feature: Solicitud Acceso — Implementation Summary

## What shipped

A secure, admin-gated registration workflow that replaces the previous direct user-creation
endpoint. Prospective users submit name, email, and role; the admin receives an email with
one-click Approve/Reject links; users are notified at every state change. Passwords are
never stored in any database table. Rejected applicants are blocked from reapplying for a
configurable number of days (default 5). Duplicate pending requests and token reuse are
both prevented at the application layer.

## Endpoints delivered

| Method | Path                                   | Status codes        |
|--------|----------------------------------------|---------------------|
| POST   | `/api/auth/register`                   | 202 / 400 / 409 / 429 |
| GET    | `/api/solicitudes/{token}/aprobar`     | 200 / 400 / 404     |
| GET    | `/api/solicitudes/{token}/rechazar`    | 200 / 400 / 404     |

## Deviations from design

None significant. Implementation followed the design document closely.

## Security improvements over previous implementation

| Previous                                    | Shipped                                               |
|---------------------------------------------|-------------------------------------------------------|
| Plaintext password stored in `solicitudes_acceso` | No password field; `SecureRandom` temp password generated and discarded on approval |
| Rejected solicitudes deleted immediately     | Retained as `RECHAZADO` with `rejected_at`; cooldown enforced |
| No duplicate-pending check                  | `findByCorreo` blocks second `PENDIENTE` for same email (409) |
| Approve/Reject tokens reusable              | State check before processing; second click → 400     |
| `UNIQUE` on `correo` blocked cooldown reuse | `UNIQUE` dropped; uniqueness enforced in application layer |

## Deferred from scope

- **IP-based rate limiting** on `POST /api/auth/register` — deferred to infrastructure
  (Cloud Armor or API Gateway rule).
- **Auto-expiry of stale `PENDIENTE` records** — no scheduler to auto-reject after N days.
- **Admin dashboard** — all admin interaction happens via email links.
- **Resend** — applicants cannot re-trigger the confirmation email.

## Acknowledged technical debt

- **No integration tests** covering the full register → approve flow end-to-end (mailer
  in mock mode for tests). A `@QuarkusTest` integration test with `quarkus-mailer` mock
  should be added.
- **HTML email templates are inlined** in Java string literals. At scale, externalizing
  them to `resources/templates/` (Qute) would improve maintainability.

## Post-ship fix: provision user split

`CreateAdminUserUseCase` was initially refactored to remove the `password` parameter,
which broke `POST /api/admin/users` — COO-created users had no way to log in.

Fix: split into two use cases with distinct responsibilities.

| Use case | Caller | Password source |
|----------|--------|-----------------|
| `CreateAdminUserUseCase` | `AdminResource` | Caller-supplied (COO defines it) |
| `ProvisionApprovedUserUseCase` | `AprobarSolicitudService` | `SecureRandom` internal; user sets own via reset link |

## Features that depend on this one

- **`user`** — `ProvisionApprovedUserUseCase` handles approval provisioning.
- **`auth`** — `AuthResource` is the entry point for the registration flow.
- Any future role-gated feature assumes users can only exist after admin approval.
