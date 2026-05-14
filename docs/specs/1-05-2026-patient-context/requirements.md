# Feature: Patient Context — Requirements

> **Naming note**: this feature is documented as `patient-context` in English. The Java package is currently `itesm.medsync.*.pacientecontexto` (Spanish). Renaming is deferred technical debt — see [conventions/naming.md](../../conventions/naming.md#legacy-spanish-packages).

## Overview

Clinical context attributes for a patient, stored as `(tipo, valor)` tag pairs. These tags are MedSync's bridge between scientific articles (whose tags share the same `(tipo, valor)` shape) and the patient's clinical state. The matching engine joins this table against `articulo_tags` to surface relevant evidence per patient. The data lives **inside MedSync** — it is not imported from the hospital DB.

## User stories

- As a **doctor**, I want to record clinical context entries for a patient (diseases, symptoms, treatments, active medications), so that the article-matching engine can find relevant evidence.
- As a **doctor**, I want to list all clinical context entries for a patient in reverse-chronological order, so that I see the most recent entries first.
- As a **doctor**, I want to delete a wrong / outdated entry, so that the matching engine doesn't surface irrelevant articles.

## Acceptance criteria (EARS format)

### Ubiquitous
- The system shall categorize every context entry as one of four `TipoClinico` values: `ENFERMEDAD`, `SINTOMA`, `TRATAMIENTO`, `MEDICAMENTO`.
- The system shall use the same `TipoClinico` enum (shared in `domain/shared/model/`) that `article` uses for its tags. This is what makes the matching join work.
- The system shall scope every CRUD operation by `patientId` (path parameter), to prevent any global / cross-patient access.
- The system shall require an authenticated caller on every endpoint.

### Event-driven
- When a valid add-context request is received for an existing patient, the system shall return `201` with the new entry.
- When a list-contexts request is received for an existing patient, the system shall return `200` with entries ordered by `createdAt DESC`.
- When a delete-context request is received for an existing patient and a context that belongs to that patient, the system shall return `204`.

### Conditional
- If the `patientId` does not exist, the system shall return `404` (`PatientNotFoundException`).
- If the `contextoId` does not exist (on delete), the system shall return `404` (`PacienteContextoNotFoundException`).
- If the `contextoId` exists but belongs to a different patient than the path's `patientId`, the system shall return `404` (`PacienteContextoNotFoundException`) — never `403` (to avoid leaking the existence of other patients' contexts).
- If `tipo` is not one of the four enum values, the system shall return `400`.
- If `valor` is blank or longer than 500 characters, the system shall return `400`.

## Non-functional requirements

- **Index on `(paciente_id, tipo, valor)`** — accelerates the matching join.
- **Hard delete** — no soft-delete; if a doctor deletes, the row is gone.
- **TipoClinico stored as string** — column type `VARCHAR(30)` with the enum name. Avoids ordinal-collision risk if enum order changes.

## Out of scope (explicit)

- Editing a context entry. To "edit", delete and re-add. *(Future: consider an update endpoint if usage warrants.)*
- Soft delete.
- Importing context from the hospital DB automatically. Doctors enter it manually today.
- Free-text search across `valor`.
- A bulk "set all contexts" endpoint.
- Pagination on the list endpoint (typical patients have <50 entries).

## Open questions

- Should we eventually derive context entries from hospital `consulta.diagnostico` / `consulta.prescripcion` automatically? *Out of scope for this feature; would be a new feature consuming `HospitalGateway`.*
- Should `Patient` aggregate `List<PacienteContexto>`? Today the relation is unidirectional (context → patient). Keeping it that way prevents loading all contexts every time we load a patient. *Settled, no change planned.*
