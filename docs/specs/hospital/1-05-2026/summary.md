# Feature: Hospital — Implementation Summary

## What shipped

Read access to the external hospital database (expedientes + consultas) plus the ability to create new consultas (with implicit expediente auto-creation). Establishes the Gateway pattern and the two-datasource topology that all future external integrations will follow.

## Endpoints delivered

| Method | Path                                | Status codes  |
|--------|-------------------------------------|---------------|
| GET    | `/api/patients/{id}/expediente`     | 200 / 404     |
| GET    | `/api/patients/{id}/consultas`      | 200 / 404     |
| GET    | `/api/consultas/{consultaId}`       | 200 / 404     |
| POST   | `/api/patients/{id}/consultas`      | 201 / 400 / 404 |

## Deviations from design

- **Auto-create expediente on POST consulta** — original plan was "read-only on expedientes". Decision relaxed during implementation. See `1-05-2026/design.md` §6.

## Deferred from scope

- Edit / delete consultas.
- Explicit endpoint to create an expediente (auto-create is the only path).
- Authentication / authorization on these endpoints.
- Real-time sync from the hospital (no webhooks or polling).
- Cache.
- Retries on hospital connection failure.
- Pagination / filters on the consultas list.

## Acknowledged technical debt

1. **JDBC connection failures to the hospital** → mapped to a generic `500`. Should be `503 Service Unavailable`. No dedicated mapper yet.
2. **Test gateway runs against H2 in `MODE=MySQL`**, not real MySQL. If we add native MySQL-only queries, we must promote to Testcontainers.
3. **Hibernate `database.generation` is deprecated** (Quarkus warning). Migrate to `schema-management.strategy` when convenient.
4. **Auto-create expediente without hospital-side authorization** — validate the policy with the hospital team before prod.
5. **No authentication** — the POST endpoint is public. Must be gated before any real deployment.

## Tests delivered

| File                              | Type                         | What it covers                                              |
|-----------------------------------|------------------------------|-------------------------------------------------------------|
| `ExpedienteClinicoTest`           | unit                         | Constructor invariants, factory, equals/hashCode            |
| `ConsultaTest`                    | unit                         | Same as above                                               |
| `GetExpedienteByPatientServiceTest` | unit (Mockito)             | Patient+expediente, patient without expediente, no patient  |
| `GetConsultasByPatientServiceTest` | unit (Mockito)              | With and without consultas, no patient                      |
| `GetConsultaByIdServiceTest`      | unit (Mockito)               | Found / not found                                           |
| `CreateConsultaServiceTest`       | unit (Mockito)               | Happy path with expediente, auto-create flow, no patient    |
| `HospitalGatewayImplTest`         | `@QuarkusTest` + `@TestTransaction` | 3 finds + saveExpediente + saveConsulta              |
| `HospitalResourceIT`              | `@QuarkusTest` + RestAssured | 14 scenarios across all 4 endpoints                         |

## Features that depend on this one

- **`alert`** (planned) — will join consultas (prescriptions + diagnoses) with article tags to generate pharmacovigilance alerts. Consumes `HospitalGateway.findConsultasByPacienteExternoId`.
- **`medication`** — eventually will parse `prescripcion` text to detect obsolete drugs.
- **`user`** — added FK on `consultas.doctor_id` is **not** in scope of the hospital feature (the hospital owns that schema). If MedSync doctors begin recording consultas under their identity, the field stays as `varchar` and we resolve it application-side.
