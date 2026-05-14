# Feature: Solicitud Acceso — Requirements

## Overview

Replace the previous direct-user-creation flow (which stored plaintext passwords and
created accounts immediately) with a secure, admin-gated approval workflow. A prospective
user submits a registration request; the admin receives an email with one-click
Approve/Reject links; the user is notified at every state transition. Passwords are never
stored in the database. Rejected applicants face a configurable cooldown before
reapplying.

## User stories

- As a **prospective user**, I want to submit an access request with my name, email, and
  intended role, so that an admin can evaluate and grant me access.
- As a **prospective user**, I want to receive a confirmation email when my request is
  submitted, so that I know it was received.
- As a **prospective user**, I want to receive an email with a password-setup link when
  approved, so that I can activate my account without the system ever storing my password.
- As a **prospective user**, I want to receive a rejection email when denied, so that I
  am not left waiting indefinitely.
- As an **admin**, I want to receive an email with Approve and Reject buttons for each
  new request, so that I can act without logging into any admin panel.
- As an **admin**, I want double-clicking Approve or Reject to be a no-op, so that
  accidental retries do not corrupt state.

## Acceptance criteria (EARS format)

### Ubiquitous
- The system shall never persist a user-supplied password in any database table.
- The system shall store all access requests in `solicitudes_acceso` with a
  server-generated UUID id and UUID token.
- The system shall record `rejected_at` when a solicitud transitions to `RECHAZADO`.

### Event-driven
- When a valid registration request is received for a new email, the system shall
  persist a `SolicitudAcceso` with state `PENDIENTE`, return `202 Accepted`, and send
  two emails: one to the admin (with Approve/Reject links) and one to the applicant
  (receipt confirmation).
- When the admin visits the approve URL, the system shall verify the solicitud is
  `PENDIENTE`, create a Firebase user with a `SecureRandom`-generated 24-character
  password, generate a Firebase password-reset link, mark the solicitud `APROBADO`,
  persist the new `User` in the database, and send the applicant an approval email
  containing the password-reset link.
- When the admin visits the reject URL, the system shall verify the solicitud is
  `PENDIENTE`, mark it `RECHAZADO` with `rejected_at = now()`, and send the applicant
  a rejection email.

### Conditional
- If the submitted email already exists in `usuarios`, the system shall return
  `409 Conflict`.
- If the submitted email already has a `PENDIENTE` solicitud, the system shall return
  `409 Conflict` with a human-readable message.
- If the submitted email was rejected fewer than `medsync.solicitudes.rejection-cooldown-days`
  days ago, the system shall return `429 Too Many Requests` stating how many days remain.
- If the submitted email was rejected and the cooldown has expired, the system shall
  delete the stale `RECHAZADO` record and allow a new submission.
- If the approve URL is visited for a solicitud that is not `PENDIENTE`, the system
  shall return `400 Bad Request`.
- If the reject URL is visited for a solicitud that is not `PENDIENTE`, the system
  shall return `400 Bad Request`.
- If the token does not match any solicitud, the system shall return `404 Not Found`.
- If Jakarta validation fails on the request body, the system shall return `400 Bad
  Request` with field-level details.
- If `rol` is not `DOCTOR` or `COO`, the system shall return `400 Bad Request`.

### State-driven
- While a solicitud is `RECHAZADO` and within the cooldown window, the system shall
  block new submissions from the same email.

## Non-functional requirements

- **Security**: passwords are never written to any persistent store; tokens are UUIDs
  (122 bits of entropy); approve/reject links are single-use via state check.
- **Configurability**: rejection cooldown is set via
  `medsync.solicitudes.rejection-cooldown-days` (default 5); no redeploy required.
- **Credential hygiene**: all SMTP credentials (`MAILER_FROM`, `MAILER_PASSWORD`),
  admin email (`ADMIN_EMAIL`), and backend URL (`BACKEND_URL`) come from environment
  variables; nothing is hardcoded.
- **Compatibility**: migration `V11` works on MySQL 8 (prod) and H2 `MODE=MySQL`
  (dev/test).
- **Idempotency**: visiting an already-processed approve/reject link returns 400,
  not 5xx; the admin page confirms the action with an HTML response.

## Out of scope (explicit)

- Rate limiting by IP on `POST /api/auth/register` (deferred to infrastructure).
- Admin UI or dashboard for managing solicitudes.
- Auto-expiry of stale `PENDIENTE` solicitudes after N days.
- Email templates stored externally (Sendgrid, Mailchimp, etc.).
- Resend-request functionality for the applicant.

## Open questions

- None outstanding. All decisions captured in `13-05-2026/design.md`.
