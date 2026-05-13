# Feature: Specialty — Requirements

## Overview

A catalog of medical specialties (Cardiología, Endocrinología, Neurología, …) used to **scope AI-driven tag extraction** in MedSync. Every doctor belongs to one specialty; every clinical-context entry and every scientific article is tagged with one specialty. Downstream features (`medical-vocabulary`, `article-ai-analysis`, `consulta-ai-analysis`) load a different vocabulary per specialty and use the specialty as a join key when matching articles to patients.

This feature owns the catalog itself, the public listing endpoint, the COO-only admin CRUD, and the schema changes that introduce `especialidad_id` on three existing tables (`usuarios`, `paciente_contexto`, `articulos_cientificos`).

## User stories

- As a **frontend developer**, I want a public list of specialties, so that I can populate dropdowns at user-admin and (future) article-classification UIs.
- As a **COO**, I want to create, rename, and deactivate specialties, so that the catalog reflects the institutions MedSync serves.
- As a **COO**, I want to assign a specialty when I create a new user, so that doctors are scoped to their domain from day one.
- As a **doctor**, I want my specialty to be carried over to every clinical-context entry I create for a patient, so that future article matching respects my domain without manual tagging.
- As a **CMO / system**, I want every persisted specialty to be referenceable by a stable, deterministic UUID, so that seed data is portable across environments and integration tests.

## Acceptance criteria (EARS format)

### Ubiquitous

- The system shall persist each specialty with a server-generated `UUID`, a unique `nombre`, and a unique `slug` (kebab-case, used as the vocabulary-file filename in the `medical-vocabulary` feature).
- The system shall seed at least 15 specialties at first migration with **deterministic UUIDs**, so they can be referenced by id across environments and tests.
- The system shall expose the seeded specialties to any authenticated user via a read-only listing endpoint.
- The system shall enforce a one-to-many relation between `Specialty` and each of `User`, `PacienteContexto`, `Article`: every row in those tables has **zero or one** specialty (MVP — no N:M).
- The system shall populate `createdAt` and `updatedAt` automatically; clients cannot set them.

### Event-driven

- When a `GET /api/especialidades` request arrives from any authenticated user, the system shall return `200 OK` with the list of **active** specialties (soft-deleted ones hidden) ordered alphabetically by `nombre`.
- When a `GET /api/especialidades/{id}` request arrives, the system shall return `200 OK` with the specialty if it exists and is active; otherwise `404 Not Found`.
- When a `POST /api/admin/especialidades` request arrives from a **COO** with a valid payload, the system shall persist a new specialty and return `201 Created` with the new resource and a `Location` header.
- When a `PUT /api/admin/especialidades/{id}` request arrives from a **COO** with a valid payload, the system shall update `nombre`, `slug`, and `descripcion` of the specialty and return `200 OK`.
- When a `DELETE /api/admin/especialidades/{id}` request arrives from a **COO**, the system shall **soft-delete** the specialty (`activo = false`) and return `204 No Content`. Existing references on users, contexts, and articles are preserved (the FK is not nullified).
- When a `POST /api/admin/users` request arrives from a COO with an `especialidadId` field, the system shall persist the new user with that specialty assigned. (Cross-feature change on `user`.)
- When a `POST /api/patients/{patientId}/contextos` request creates a new `PacienteContexto` entry, the system shall **inherit the authenticated user's specialty** as the entry's `especialidadId` (without any client input). (Cross-feature change on `patient-context`.)

### State-driven

- While a specialty is soft-deleted, the system shall hide it from `GET /api/especialidades` listings and from `GET /api/especialidades/{id}` lookups.
- While a specialty is soft-deleted, the system shall **still** preserve its rows so that historical references on users, articles, and contexts remain resolvable by id.

### Conditional

- If a non-COO authenticated user calls any `POST /api/admin/especialidades`, `PUT /api/admin/especialidades/{id}`, or `DELETE /api/admin/especialidades/{id}` endpoint, the system shall return `403 Forbidden`.
- If an unauthenticated request reaches any specialty endpoint, the system shall return `401 Unauthorized`.
- If a `POST /api/admin/especialidades` payload contains a `nombre` or `slug` already in use by an **active** specialty, the system shall return `409 Conflict`.
- If a `POST` or `PUT` payload fails Jakarta validation (missing `nombre`, length exceeded, `slug` not matching the kebab-case pattern), the system shall return `400 Bad Request` with field-level details.
- If `POST /api/admin/users` is invoked with an `especialidadId` that does not match an active specialty, the system shall return `404 Not Found` (`SpecialtyNotFoundException`).
- If a write to a specialty-referencing column is attempted with an `especialidadId` that does not exist, the system shall reject the operation before the database round-trip with `404 Not Found`.
- If `slug` collides with the slug of a soft-deleted specialty, the system shall **still** return `409 Conflict` (slugs are globally unique across all rows because they back filesystem resources in `medical-vocabulary`).

## Non-functional requirements

- **Compatibility**: schema and seed SQL run on both MySQL 8 (prod) and H2 in `MODE=MySQL` (dev / test).
- **Determinism**: seed UUIDs are fixed strings written into the migration script — re-running on an existing DB is a no-op (`ON DUPLICATE KEY UPDATE`).
- **Listing performance**: `GET /api/especialidades` returns < 50 ms for the seeded catalog (~15 rows, no joins).
- **Auditability**: timestamps are server-set; soft deletes leave a `deleted_at`-like trail via `activo = false` + `updated_at`.
- **Test coverage**: ≥ 80% on `domain/specialty` and `application/specialty`.

## Out of scope (explicit)

- Many-to-many between `Specialty` and any of `User` / `PacienteContexto` / `Article`. (One specialty per row is an MVP decision — see [conventions/naming.md](../../conventions/naming.md) and design.md.)
- Re-assigning a user's specialty (`PATCH /api/admin/users/{id}`). Users keep the specialty assigned at creation time. Reassignment is deferred to a future user-spec revision.
- Bulk import of specialties.
- Historic vocabulary versioning per specialty (vocabularies are a code-managed artifact — see the `medical-vocabulary` spec).
- Backfill of `especialidad_id` on existing rows in `usuarios`, `paciente_contexto`, and `articulos_cientificos`. The new columns are **nullable** so existing rows remain valid; assignment happens organically as new users are created and new context / article rows are tagged.

## Open questions

- None blocking. Outstanding decisions are documented in `design.md`.
