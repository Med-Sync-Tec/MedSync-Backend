# Feature: Patient — Requirements

## Overview

CRUD for the patients managed by MedSync. A patient is the central entity that ties together clinical context, hospital records, and pharmacovigilance alerts. This feature exposes create / read / soft-delete endpoints; editing is intentionally out of scope.

## User stories

- As a **doctor**, I want to register a new patient, so that I can track their clinical context in MedSync.
- As a **doctor**, I want to list active patients, so that I can navigate to their records.
- As a **doctor**, I want to retrieve a patient by id, so that I can view their details.
- As a **doctor**, I want to soft-delete a patient, so that they disappear from active lists but their data is preserved for audit.

## Acceptance criteria (EARS format)

### Ubiquitous
- The system shall persist patients with a server-generated UUID.
- The system shall enforce that `expedienteExternoId` is globally unique (including soft-deleted patients).
- The system shall populate `createdAt` and `updatedAt` automatically.

### Event-driven
- When a valid create-patient request is received, the system shall return `201 Created` with the persisted patient resource and a `Location` header.
- When a get-by-id request for an active patient is received, the system shall return `200 OK` with the patient.
- When a list request is received, the system shall return `200 OK` with all active (non-soft-deleted) patients.
- When a delete request for an existing patient is received, the system shall mark `activo = false` and return `204 No Content`.

### Conditional
- If `expedienteExternoId` already exists for any patient (active or soft-deleted), the system shall return `409 Conflict`.
- If `fechaNacimiento` is in the future, the system shall return `400 Bad Request`.
- If the derived age exceeds 150 years, the system shall return `400 Bad Request`.
- If a get-by-id, delete, or list-related request references a non-existent id, the system shall return `404 Not Found`.
- If the request payload fails Jakarta validation (missing required fields, length exceeded), the system shall return `400 Bad Request` with field-level details.

### State-driven
- While a patient is soft-deleted, the system shall hide them from list and get-by-id endpoints.

## Non-functional requirements

- **Compatibility**: works on both MySQL 8 (prod) and H2 in `MODE=MySQL` (dev / test).
- **Auditability**: timestamps are immutable from client input — only Hibernate sets them.
- **Test coverage**: ≥80% on `domain/patient` and `application/patient`.

## Out of scope (explicit)

- Editing (`PUT` / `PATCH`).
- Restoring soft-deleted patients.
- Pagination or filters on the list endpoint.
- Foreign-key constraint on `medico_id` (deferred to the `user` feature migration).
- Authentication / authorization (the `user` feature handles this once integrated).

## Open questions

- None outstanding. Decisions captured in `1-05-2026/design.md`.
