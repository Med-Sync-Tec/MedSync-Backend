# Feature: Specialty — Implementation Summary

## What shipped

The medical-specialty catalog ships as a full vertical slice: a new `Specialty` domain aggregate with kebab-case slug invariants, five use-case services (`ListActive`, `GetById`, `Create`, `Update`, `SoftDelete`), a Panache-backed `SpecialtyRepositoryImpl` with `@SQLRestriction("activo = true")`, and two REST resources — public reads under `/api/especialidades` and COO-only CRUD under `/api/admin/especialidades`. Sixteen specialties are seeded with deterministic UUIDs (Cardiología, Endocrinología, Neurología, Oncología, Pediatría, Gastroenterología, Neumología, Dermatología, Ginecología, Urología, Psiquiatría, Reumatología, Oftalmología, Hematología, Medicina Interna, Infectología).

Three migrations land in this snapshot: **V10** creates the table, **V11** seeds it, **V12** introduces a nullable `especialidad_id BINARY(16)` FK column on `usuarios`, `paciente_contexto`, and `articulos_cientificos` and **atomically drops** the legacy free-text `usuarios.especialidad VARCHAR(100)` column that an earlier commit had introduced in `V9`.

Cross-feature impact propagates the new identifier:
- `User` carries `UUID especialidadId` (replacing the prior `String especialidad`); the `POST /api/admin/users` flow optionally accepts an `especialidadId` and validates it against the catalog before Firebase user creation.
- `PacienteContexto` gains a nullable `UUID especialidadId` that is silently inherited from the authenticated user on every `POST /api/patients/{id}/contextos`.
- `Article` gains a nullable `UUID especialidadId` field (manual create still leaves it `null`; AI-driven population is the next feature's job).
- The legacy `DashboardRepositoryImpl.findPorEspecialidad(...)` query was refactored from "join `articulo_tags.valor` against `user.especialidad` free-text" to "filter `ArticleEntity.especialidadId` by `user.especialidadId`" — semantically correct under the new model, but returns empty for all PubMed-imported articles until feature 3 lands.

## Endpoints delivered

| Method | Path                                  | Request DTO              | Response DTO               | Status codes                          |
|--------|---------------------------------------|--------------------------|----------------------------|---------------------------------------|
| GET    | `/api/especialidades`                 | —                        | `List<SpecialtyResponse>`  | 200 / 401                             |
| GET    | `/api/especialidades/{id}`            | —                        | `SpecialtyResponse`        | 200 / 401 / 404                       |
| POST   | `/api/admin/especialidades`           | `CreateSpecialtyRequest` | `SpecialtyResponse`        | 201 / 400 / 401 / 403 / 409           |
| PUT    | `/api/admin/especialidades/{id}`      | `UpdateSpecialtyRequest` | `SpecialtyResponse`        | 200 / 400 / 401 / 403 / 404 / 409     |
| DELETE | `/api/admin/especialidades/{id}`      | —                        | —                          | 204 / 401 / 403 / 404                 |

Swagger tags: `Specialties` for the two public reads, `Admin` for the three COO-only writes. All endpoints require an authenticated `AuthenticatedUserContext`; admin endpoints additionally enforce `KnownRoles.COO`.

Cross-feature endpoint changes:
- `POST /api/admin/users` accepts an optional `especialidadId` field; missing-id is rejected with `SpecialtyNotFoundException → 404`.
- `POST /api/patients/{patientId}/contextos` no longer needs a body change; the doctor's specialty is read from `AuthenticatedUserContext` and propagated into the persisted row.
- Response DTOs `UserResponse` and `PacienteContextoResponse` gained `especialidadId` (and `especialidadNombre`, nullable, on `UserResponse`).

## Deviations from design

1. **Migration numbering shifted by +1.** The design specified `V9 / V10 / V11`; reality used `V10 / V11 / V12` because `V9__Add_Especialidad_And_Tracking_Table.sql` was already committed in `8048d3e` (`feat(pubmed): reducir cantidad de articulos consultados…`). The committed V9 introduced (a) a legacy free-text `usuarios.especialidad VARCHAR(100)` column and (b) an unrelated `usuario_articulos_leidos` tracking table. Reviewed and approved during planning.

2. **`V12` atomically drops the legacy free-text column.** Not in the original spec because the spec did not anticipate V9's existence. The drop is part of the same migration that adds the FK column, keeping the schema in one consistent state. The two pre-existing call sites (`User.especialidad` getter consumers and `DashboardRepositoryImpl.findPorEspecialidad`) were refactored to use the new FK. No backfill was needed (no production data).

3. **Seed migration syntax: `X'...'` literals instead of `UNHEX('...')`, no `ON DUPLICATE KEY UPDATE`.** The design suggested the MySQL-flavored form, but H2 in `MODE=MySQL` does not provide `UNHEX` and rejects `ON DUPLICATE KEY UPDATE`. Discovered at first run as a Flyway boot failure: `Function "unhex" not found … V11__seed_especialidades.sql`, which cascaded into every `@QuarkusTest` skipping (132 of 352). Rewrote using the `X'00000000…'` binary-literal form that matches the existing `V3__Seed_Roles.sql` precedent and works on both H2 and MySQL. The `ON DUPLICATE KEY UPDATE` clause was dropped as unnecessary — Flyway's version tracking already prevents re-runs.

4. **`V12` split into per-statement ALTER TABLE clauses.** The design wrote each table's column-add and constraint-add as a single multi-clause `ALTER TABLE usuarios ADD COLUMN …, ADD CONSTRAINT …`. H2 rejects that form even in `MODE=MySQL`, requiring one action per `ALTER TABLE` statement. Surfaced the same way as deviation #3 — Flyway boot failure on the first `@QuarkusTest` after the seed fix landed. Split each into separate `ALTER TABLE ADD COLUMN`, `ALTER TABLE ADD CONSTRAINT`, and `CREATE INDEX` statements. Behavior is identical on MySQL.

5. **`User.especialidad` rename was zero-churn.** The design specified renaming the field and adjusting every call site. In practice, all 47 existing call sites passed `null` to the third positional argument of `User.create(...)` and `new User(...)`, and the null literal compiles identically against `String` or `UUID`. The constructor and factory signatures kept their length; only the parameter type changed. No test code needed to be updated for the rename itself.

6. **`User.withRoleAndSpecialty` `@NamedEntityGraph` not introduced.** The design's cross-feature impact table called for a new graph that eagerly fetches both role and specialty. The existing `User.withRole` graph was kept unchanged because (a) no current query joins on the specialty's row data and (b) the `AuthenticatedUserContext` flow only needs `User.especialidadId` (a scalar), which `UserPersistenceMapper.toDomain` already populates from `UserEntity`. The graph addition is deferred to feature 3 (`article-ai-analysis`), where the matching query against `paciente_contexto.especialidad_id` actually benefits.

7. **`Article.withAiAnalysis(...)` not added.** Deferred to feature 3 per its `tasks.md`. The current snapshot only adds the field, getter, mapper, and entity column.

8. **Admin endpoints live in a dedicated `SpecialtyAdminResource`.** The design implied two `@Tag` groupings on one resource class, but JAX-RS resolves a single class against a single `@Path`. Created `interfaces/rest/admin/SpecialtyAdminResource.java` at `/api/admin/especialidades` alongside the existing `AdminResource` (user-admin), and kept `interfaces/rest/specialty/SpecialtyResource.java` at `/api/especialidades` for the public reads. Behavior matches the design; only file organization differs.

9. **`PacienteContexto` / `Article` constructors use backwards-compatible overloads.** Both domain models added the new `especialidadId` field, but the original constructor signatures were preserved (delegating to a new constructor that defaults the specialty to `null`). This kept the diff in `*PersistenceMapper` and existing tests to zero rows changed. The new factory overloads (`PacienteContexto.create(pacienteId, tipo, valor, especialidadId)`) sit alongside the originals.

## Deferred from scope

- **Specialty editing on existing users.** No `PATCH /api/admin/users/{id}` endpoint exists. Doctors keep the specialty assigned at creation. Re-assignment requires a dedicated future spec with audit considerations.
- **Backfill of `especialidad_id` on existing rows.** The new column is nullable on all three referencing tables. Existing rows remain valid; they are simply invisible to AI-driven matching until re-tagged.
- **`User.withRoleAndSpecialty` entity graph.** Deferred to feature 3 where it pays off.
- **Soft-deleted specialty rendering in admin UI.** A soft-deleted specialty stays referenceable by FK on existing rows but is hidden from `GET /api/especialidades`. Admin UIs that render historic context-entry tags will need a "show including deleted" mode — out of scope here.
- **N:M between `Specialty` and `User` / `Article` / `PacienteContexto`.** MVP locks each row to one specialty.

## Acknowledged technical debt

- **`DashboardRepositoryImpl.findPorEspecialidad(...)` is now functionally empty for legacy articles.** It returns articles where `especialidad_id = <user's especialidad_id>`, but no article currently has a non-null `especialidad_id` — PubMed-imported articles will only gain one when feature 3 (`article-ai-analysis`) runs over them. Users with a specialty will see an empty "por especialidad" feed until then. Tracked as part of feature 3's smoke test.
- **The `usuario_articulos_leidos` tracking table** introduced by V9 is unrelated to specialty and was not touched. Its lifecycle is owned by whatever feature drives the "novedades" / "no leídos" dashboard.
- **No `@NamedEntityGraph("User.withRoleAndSpecialty")`.** Documented as deferred (deviation #5). Will be added with feature 3.
- **`SpecialtyAdminResource` duplicates the COO-check pattern** from `AdminResource`. A `@RolesAllowed("COO")` or a shared filter would consolidate it. Acceptable while only two resources need the check.

## Tests

| File                                                                                       | Type             | What it covers                                                                                                          |
|--------------------------------------------------------------------------------------------|------------------|-------------------------------------------------------------------------------------------------------------------------|
| `domain/specialty/model/SpecialtyTest`                                                     | unit             | Constructor invariants, slug regex (positive + negative cases), length caps, factory, `softDelete`, `with*` immutability, `equals/hashCode` |
| `application/specialty/ListActiveSpecialtiesServiceTest`                                   | unit (Mockito)   | Repository delegation, empty list                                                                                       |
| `application/specialty/GetSpecialtyByIdServiceTest`                                        | unit (Mockito)   | Hit, miss → `SpecialtyNotFoundException`                                                                                |
| `application/specialty/CreateSpecialtyServiceTest`                                         | unit (Mockito)   | Happy, duplicate `nombre`, duplicate `slug` (global), invalid domain data, `null` descripcion                           |
| `application/specialty/UpdateSpecialtyServiceTest`                                         | unit (Mockito)   | Happy, miss, same-value no-op, `nombre` collision, `slug` collision, id preservation                                    |
| `application/specialty/SoftDeleteSpecialtyServiceTest`                                     | unit (Mockito)   | Happy, miss                                                                                                             |
| `infrastructure/persistence/specialty/SpecialtyRepositoryImplTest`                         | integration      | Round-trip save, `findByUuid`, `findBySlug`, alphabetic `findAllActive`, `@SQLRestriction` filtering, native-SQL uniqueness check across all rows, update-via-save (upsert) |
| `interfaces/rest/specialty/SpecialtyResourceIT`                                            | REST integration | 11 scenarios covering 200, 201, 204, 400, 401, 403, 404, 409 across read + admin paths; round-trip create-then-delete  |

Cross-feature test updates:
- `application/user/CreateAdminUserServiceTest` — updated to the 5-arg `execute(...)` signature; added two new cases: happy path with `especialidadId` (validates via mocked `GetSpecialtyByIdUseCase`) and unknown specialty id (404 before Firebase touch).
- `application/pacientecontexto/AddPacienteContextoServiceTest` — updated to the 4-arg signature; added one new case asserting the caller's specialty is propagated to the persisted row.

Coverage targets per `conventions/testing.md` (≥ 80% on `domain/specialty` and `application/specialty`) — pending operator confirmation via `./mvnw test jacoco:report`.

## Next features that depend on this one

- **`medical-vocabulary` (feature 2)** — loads JSON vocabulary files keyed by `Specialty.slug`; the loader queries `SpecialtyRepository.findAllActive()` at boot.
- **`article-ai-analysis` (feature 3)** — populates `Article.especialidadId` via Claude classification; updates the `GetMatchingArticlesByPatient` SQL to filter on `articulo.especialidad_id = paciente_contexto.especialidad_id`. Will also retire the technical-debt note in `DashboardRepositoryImpl.findPorEspecialidad`.
- **`consulta-ai-analysis` (feature 4)** — depends on `User.especialidadId` being set on the authenticated doctor; rejects analysis with `UserHasNoSpecialtyException` (400) when the field is null.
- **Future:** if `Article.withAiAnalysis(...)`, the `withRoleAndSpecialty` graph, or a `PATCH` to re-assign user specialties is needed, those slot in as cross-feature deltas on top of this snapshot.
