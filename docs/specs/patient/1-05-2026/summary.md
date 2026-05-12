# Feature: Patient — Implementation Summary

## What shipped

Full CRUD-minus-update for patients, with soft delete, global uniqueness on `expedienteExternoId` (including soft-deleted rows), and the foundational `Patient` aggregate that all subsequent features build on.

## Endpoints delivered

| Method | Path                  | Status codes  |
|--------|-----------------------|---------------|
| POST   | `/api/patients`       | 201 / 400 / 409 |
| GET    | `/api/patients`       | 200           |
| GET    | `/api/patients/{id}`  | 200 / 404     |
| DELETE | `/api/patients/{id}`  | 204 / 404     |

## Deviations from design

None significant. Implementation followed the design closely.

## Deferred from scope

- **Editing** (`PUT` / `PATCH`) — intentionally cut.
- **Restoring soft-deleted patients** — feasible because `save` bypasses `@SQLRestriction`, but no UI / use case yet.
- **Pagination / filters on list** — single-tenant MVP; full table fits in one response. Revisit when row count grows.
- **FK on `medico_id`** — added later in `V4__add_patient_medico_fk.sql` after the `user` feature shipped.

## Acknowledged technical debt

- **No authorization**. The endpoints are public. The `user` + `security` features now provide a Firebase-based auth filter, but the patient endpoints have not been gated yet. Pre-prod blocker.
- **List endpoint unbounded**. With ~100 patients today it's fine. At 10k it will hurt — add pagination.

## Tests delivered

| File                          | Type             | What it covers                                          |
|-------------------------------|------------------|---------------------------------------------------------|
| `PatientTest`                 | unit, JUnit pure | Constructor invariants, `softDelete`, equals/hashCode   |
| `CreatePatientServiceTest`    | unit, Mockito    | Duplicate detection, happy path                         |
| `GetPatientByIdServiceTest`   | unit, Mockito    | Found / not found                                       |
| `ListActivePatientsServiceTest` | unit, Mockito  | Filtering of soft-deleted                               |
| `DeletePatientServiceTest`    | unit, Mockito    | Soft-delete vs not-found                                |
| `PatientRepositoryImplTest`   | `@QuarkusTest` + `@TestTransaction` | `@SQLRestriction` filtering, native query bypass, timestamp population |
| `PatientResourceIT`           | `@QuarkusTest` + RestAssured | All four endpoints + status codes + end-to-end soft-delete flow |

## Features that depend on this one

- **`hospital`** — `expedienteExternoId` is the bridge to `expedientes_clinicos.paciente_externo_id`.
- **`patient-context`** — `paciente_id` FK points to `patients.id`.
- **`article`** — `GetMatchingArticlesByPatient` reads patient context to drive its query.
- **`alert`** (planned) — every alert is scoped to a `Patient`.
