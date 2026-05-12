# Feature: Patient Context — Implementation Summary

## What shipped

Three endpoints — add, list, delete — for managing clinical context tags scoped to a single patient. Each context is a `(TipoClinico, valor)` pair that the `article` matching engine joins against to surface relevant evidence. Doctors enter context manually; nothing is auto-imported from the hospital DB.

## Endpoints delivered

| Method | Path                                                | Status codes              |
|--------|-----------------------------------------------------|---------------------------|
| POST   | `/api/patients/{patientId}/contextos`               | 201 / 400 / 401 / 404     |
| GET    | `/api/patients/{patientId}/contextos`               | 200 / 401 / 404           |
| DELETE | `/api/patients/{patientId}/contextos/{contextoId}`  | 204 / 401 / 404           |

## Migrations applied

- `V5__create_paciente_contexto_table.sql`

## Deviations from design

None significant.

## Deferred from scope

- Editing entries (delete + re-add is the workflow).
- Soft delete.
- Auto-import from `consulta.diagnostico` / `prescripcion`.
- Free-text search across `valor`.
- Pagination.
- A bulk "replace all contexts" endpoint.

## Acknowledged technical debt

1. **Java package still in Spanish** (`pacientecontexto`). Rename to `patientContext` is deferred — see `conventions/naming.md`.
2. **`valor` is free text** with no normalization. Two doctors may enter `"diabetes mellitus"` and `"DM"` for the same condition — matching would treat them as different. Future improvement: a controlled vocabulary or fuzzy match.
3. **No history of removed entries** — when a doctor deletes a context, we lose the record. If pharmacovigilance audit requires preserving "what the patient was tagged with at time T", we'll need a separate audit table.
4. **`TipoClinico` enum is closed**. Adding a new value requires Java change + coordinated `article` work. Acceptable today.

## Tests delivered

| File                                          | Type                  | What it covers                                       |
|-----------------------------------------------|-----------------------|------------------------------------------------------|
| `PacienteContextoTest`                        | unit                  | Invariants                                           |
| 3 `*ServiceTest`                              | unit (Mockito)        | Patient lookup, ownership check, NotFound paths      |
| `PacienteContextoRepositoryImplTest`          | `@QuarkusTest`        | Order by `createdAt DESC`, FK constraint, ENUM column |
| `PacienteContextoResourceIT`                  | RestAssured           | All 3 endpoints + ownership-mismatch → 404           |

## Features that depend on this one

- **`article`** — `GetMatchingArticlesByPatient` joins `paciente_contexto` ⨯ `articulo_tags` on `(tipo, valor)`.
- **`alert`** (planned) — alerts will consider both clinical context and consultation history (the latter from `HospitalGateway`).
