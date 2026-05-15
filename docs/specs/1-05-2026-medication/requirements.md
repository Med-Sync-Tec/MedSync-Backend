# Feature: Medication — Requirements

> **Naming note**: this feature is documented as `medication` in English. The Java package is currently `itesm.medsync.*.medicamento` (Spanish). Renaming is deferred technical debt — see [conventions/naming.md](../../conventions/naming.md#legacy-spanish-packages).

## Overview

Catalog of medications (drugs) used by the pharmacovigilance engine. Each medication has a **status** — `VIGENTE` (currently approved), `EN_REVISION` (under reevaluation), or `OBSOLETO` (withdrawn) — that drives alerting. When an article tags an obsolete drug or a doctor prescribes one, the alert system uses this catalog to know.

## User stories

- As an **admin / pharmacist**, I want to add new medications to the catalog, so that the system knows about them.
- As an **admin / pharmacist**, I want to list medications with filters (by name substring, by status), so that I can review the catalog.
- As an **admin / pharmacist**, I want to view a single medication's details, so that I can verify its metadata.
- As an **admin / pharmacist**, I want to update a medication's name, status, or description, so that I can keep the catalog current.
- As an **admin / pharmacist**, I want to change just the status quickly (a frequent operation), so that I can flag a newly withdrawn drug without editing other fields.
- As an **admin / pharmacist**, I want to delete a medication, so that mistakes can be removed.

## Acceptance criteria (EARS format)

### Ubiquitous
- The system shall enforce a global `UNIQUE` constraint on medication `nombre`.
- The system shall reference medication statuses by `UUID` against the `medicamento_estados` table (status is a row, not an enum).
- The system shall ship three seed statuses (`VIGENTE`, `EN_REVISION`, `OBSOLETO`) with deterministic UUIDs.
- The system shall ship 15 seed medications spanning all three statuses, sourced from clinically referenced drugs.
- The system shall require authentication on every endpoint of this feature.

### Event-driven
- When a valid create-medication request is received, the system shall create the record with status `VIGENTE` and return `201`.
- When a list request is received, the system shall return `200` with a paginated list (default `page=0, size=10`), filtered by optional `nombre` (LIKE) and `estado` (exact match by name).
- When a get-by-id request is received for an existing medication, the system shall return `200` with the medication and its current status name.
- When a valid update request is received, the system shall update name + status + description and return `200`.
- When a status-only update (`PATCH /{id}/estado`) is received, the system shall update only the status and return `200`.
- When a delete request is received, the system shall hard-delete the medication and return `204`.

### Conditional
- If `nombre` already exists on a different medication, the system shall return `409` (`DuplicateMedicamentoException`).
- If `estado` (name) does not exist, the system shall return `404` (`EstadoNotFoundException`).
- If the medication id does not exist, the system shall return `404` (`MedicamentoNotFoundException`).
- If the request payload fails validation, the system shall return `400`.
- If the caller is not authenticated, the system shall return `401`.

## Non-functional requirements

- **Pagination**: response must include `totalElements` and `totalPages` so the frontend can render page controls.
- **N+1 avoidance**: the status row must be fetched in the same query as the medication (`@NamedEntityGraph`).

## Out of scope (explicit)

- Soft delete. Deletion is physical.
- Audit history of status transitions.
- Bulk import (CSV / API).
- Free-text search across `descripcion`.
- A taxonomy of medication classes (antibiotics, NSAIDs, etc.). Could be added later as another reference table.

## Open questions

- Should status transitions be auditable? (e.g. table `medicamento_estado_changes`.) *Not blocking; revisit when pharmacovigilance regulators require it.*
- Should we add a soft-delete flag? *Today the answer is no — admins should not be deleting frequently. Revisit if it happens.*
