# Feature: Medication — Implementation Summary

## What shipped

Full CRUD on a medication catalog, with a separate status reference table (`VIGENTE` / `EN_REVISION` / `OBSOLETO`). Paginated list with optional filters by name (LIKE) and status (exact match). A dedicated `PATCH /estado` endpoint optimizes the most frequent operation. 15 seeded drugs across the three statuses give the catalog non-empty initial state.

## Endpoints delivered

| Method | Path                              | Status codes                  |
|--------|-----------------------------------|-------------------------------|
| GET    | `/api/medicamentos`               | 200 / 401                     |
| GET    | `/api/medicamentos/{id}`          | 200 / 401 / 404               |
| POST   | `/api/medicamentos`               | 201 / 400 / 401 / 409         |
| PUT    | `/api/medicamentos/{id}`          | 200 / 400 / 401 / 404 / 409   |
| DELETE | `/api/medicamentos/{id}`          | 204 / 401 / 404               |
| PATCH  | `/api/medicamentos/{id}/estado`   | 200 / 400 / 401 / 404         |

## Migrations applied

- `V7__create_medicamentos_schema.sql`
- `V8__seed_medicamentos.sql` (3 status rows + 15 medication rows)

## Deviations from design

None significant.

## Deferred from scope

- Soft delete.
- Audit table for status transitions.
- Bulk import (CSV / API).
- Free-text search on `descripcion`.
- Class / category taxonomy (antibiotics, NSAIDs, ...).
- Role-based authorization (any authenticated user can write today).

## Acknowledged technical debt

1. **Java package still in Spanish** (`medicamento`). Rename to `medication` is documented but deferred — high churn, large diff, scheduled as its own PR.
2. **No auditing** of who created / updated / deleted what. Will hurt if a regulator asks for a "who changed Rofecoxib to OBSOLETO" report.
3. **Default status hard-coded** (`VIGENTE`) in `CreateMedicamentoService`. Acceptable today; would be a configuration knob if other tenants joined.
4. **All authenticated users can write** — no role check. Should be gated to `COO` or a new `PHARMACIST` role.

## Tests delivered

| File                                      | Type                          | What it covers                                       |
|-------------------------------------------|-------------------------------|------------------------------------------------------|
| `MedicamentoTest`, `MedicamentoEstadoTest` | unit                         | Invariants, factories, equals/hashCode               |
| 6 `*ServiceTest`                          | unit (Mockito)                | Duplicate handling, status resolution, NotFound paths |
| `MedicamentoRepositoryImplTest`           | `@QuarkusTest` + `@TestTransaction` | EntityGraph eager-load, pagination COUNT correctness |
| `MedicamentoResourceIT`                   | RestAssured                   | All 6 endpoints + status codes + auth gating         |

## Features that depend on this one

- **`alert`** (planned) — will consume `MedicamentoRepository.findByNombre(...)` and `findAllByEstado('OBSOLETO')` to evaluate prescriptions and flag obsolete drugs.
- **PubMed sync** (in `article`) could one day cross-reference extracted drug names against this catalog.
