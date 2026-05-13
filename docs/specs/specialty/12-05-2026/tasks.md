# Feature: Specialty — Implementation Tasks

Execute in order. Each item ≈ one commit. Use TDD per [conventions/testing.md](../../../conventions/testing.md).

Per project preference, **the operator runs the test commands**. Each `→ RED` / `→ GREEN` step describes what should happen when you (the operator) execute `./mvnw test`; the agent does not execute them.

## Domain

- [ ] Write `SpecialtyTest` (unit, pure JUnit) covering constructor invariants (`id`, `nombre`, `slug` not null/blank; `slug` matches kebab-case regex; `nombre` ≤ 100; `slug` ≤ 60; `descripcion` ≤ 500), the `softDelete()` immutability behavior, and each `with*` copy-on-write method → **RED**
- [ ] Implement `domain/specialty/model/Specialty.java` + `domain/specialty/exception/InvalidSpecialtyDataException.java` → **GREEN**
- [ ] Define `domain/specialty/repository/SpecialtyRepository.java` with the six methods listed in `design.md`
- [ ] Define `domain/specialty/usecase/ListActiveSpecialtiesUseCase.java`
- [ ] Define `domain/specialty/usecase/GetSpecialtyByIdUseCase.java`
- [ ] Define `domain/specialty/usecase/CreateSpecialtyUseCase.java`
- [ ] Define `domain/specialty/usecase/UpdateSpecialtyUseCase.java`
- [ ] Define `domain/specialty/usecase/SoftDeleteSpecialtyUseCase.java`
- [ ] Add `domain/specialty/exception/SpecialtyNotFoundException.java`
- [ ] Add `domain/specialty/exception/DuplicateSpecialtyException.java` (carries the offending field name — `"nombre"` or `"slug"` — in the message)

## Application

- [ ] Write `ListActiveSpecialtiesServiceTest` (Mockito) covering happy-path delegation and alphabetic order assumption → **RED**
- [ ] Implement `application/specialty/ListActiveSpecialtiesService.java` → **GREEN**
- [ ] Write `GetSpecialtyByIdServiceTest` covering hit and `SpecialtyNotFoundException` on miss → **RED**
- [ ] Implement `application/specialty/GetSpecialtyByIdService.java` → **GREEN**
- [ ] Write `CreateSpecialtyServiceTest` covering: happy path, duplicate `nombre` → `DuplicateSpecialtyException`, duplicate `slug` (across all rows) → `DuplicateSpecialtyException`, propagation of `InvalidSpecialtyDataException` from the constructor → **RED**
- [ ] Implement `application/specialty/CreateSpecialtyService.java` → **GREEN**
- [ ] Write `UpdateSpecialtyServiceTest` covering: happy path, miss → `SpecialtyNotFoundException`, `nombre` collision with another active row → `DuplicateSpecialtyException`, `slug` collision (any row) → `DuplicateSpecialtyException`, allowing a no-op update of the same row to its own current values → **RED**
- [ ] Implement `application/specialty/UpdateSpecialtyService.java` → **GREEN**
- [ ] Write `SoftDeleteSpecialtyServiceTest` covering: happy path → persisted row has `activo = false`, miss → `SpecialtyNotFoundException` → **RED**
- [ ] Implement `application/specialty/SoftDeleteSpecialtyService.java` → **GREEN**

## Persistence

- [ ] Write Flyway migration `src/main/resources/db/migration/V9__create_especialidades_table.sql` (matches the SQL in `design.md`)
- [ ] Write Flyway migration `src/main/resources/db/migration/V10__seed_especialidades.sql` (16 idempotent inserts with deterministic UUIDs)
- [ ] Write `SpecialtyRepositoryImplTest` (`@QuarkusTest` + `@TestTransaction`) covering:
  - `save` round-trips through `toEntity` / `toDomain`
  - `findByUuid` returns empty for unknown id and Optional for known id
  - `findBySlug` resolves seeded slugs
  - `findAllActive` returns only `activo = true` rows ordered by `nombre`
  - `existsByNombreActive` returns `false` for a `nombre` that exists only on a soft-deleted row
  - `existsBySlugAcrossAllRows` returns `true` for a slug present only on a soft-deleted row
  - `@SQLRestriction` hides soft-deleted rows from listing and from `existsByNombreActive`
  → **RED**
- [ ] Implement `infrastructure/persistence/specialty/SpecialtyEntity.java` with `@SQLRestriction("activo = true")`, audit timestamps, and the `idx_especialidades_activo` index documented in `design.md`
- [ ] Implement `infrastructure/persistence/specialty/SpecialtyPersistenceMapper.java` (static `toEntity` / `toDomain` / `toDomainList`)
- [ ] Implement `infrastructure/persistence/specialty/SpecialtyRepositoryImpl.java` (implements `SpecialtyRepository` **and** `PanacheRepository<SpecialtyEntity>`; native query for `existsBySlugAcrossAllRows`) → **GREEN**
- [ ] Verify Hibernate `validate` boots cleanly against the new schema (no `@Entity` ↔ DDL mismatch)

## REST

- [ ] Write `SpecialtyResourceIT` (`@QuarkusTest` + RestAssured) covering one test per status code (200 list, 200 get-by-id, 404 unknown id, 401 unauthenticated, 403 non-COO on admin endpoints, 201 create, 400 validation failure on `slug` regex, 409 duplicate `nombre`, 409 duplicate `slug`, 200 update, 204 soft-delete, listing returns alphabetic order) → **RED**
- [ ] Implement `interfaces/rest/specialty/CreateSpecialtyRequest.java` (public Jakarta Validation fields per `design.md`)
- [ ] Implement `interfaces/rest/specialty/UpdateSpecialtyRequest.java`
- [ ] Implement `interfaces/rest/specialty/SpecialtyResponse.java` (record)
- [ ] Implement `interfaces/rest/specialty/SpecialtyRestMapper.java` (`toResponse(Specialty)` + `toResponseList(List<Specialty>)`)
- [ ] Implement `interfaces/rest/specialty/SpecialtyResource.java` with the five endpoints, COO check on admin paths via `AuthenticatedUserContext`, `@Tag("Specialties")` / `@Tag("Admin")` and `@APIResponse` annotations → **GREEN**
- [ ] Wire the three new exceptions into `infrastructure/config/GlobalExceptionHandler` (`SpecialtyNotFoundException → 404 SPECIALTY_NOT_FOUND`, `DuplicateSpecialtyException → 409 DUPLICATE_SPECIALTY`, `InvalidSpecialtyDataException → 400 INVALID_SPECIALTY_DATA`)
- [ ] Verify Swagger UI lists the five endpoints under the right tags

## Cross-feature impact — schema

- [ ] Write Flyway migration `src/main/resources/db/migration/V11__add_especialidad_id_to_existing_tables.sql` (adds the nullable FK column + FK constraint + index to `usuarios`, `paciente_contexto`, and `articulos_cientificos`, as specified in `design.md`)
- [ ] Verify the migration on a freshly built H2 (the existing tables must already be present from V1–V8; the column add must not error on rows with `NULL`)

## Cross-feature impact — `user` feature

- [ ] Update `UserEntity`: add `@ManyToOne(fetch = LAZY)` to `SpecialtyEntity` on `especialidad_id` (`insertable=false, updatable=false`) **and** a scalar `UUID especialidadId` column for write paths; add `@NamedEntityGraph("User.withRoleAndSpecialty")`
- [ ] Update `User` domain model: add nullable `UUID especialidadId`; update constructor signature; update `User.create(...)` factory; update existing `UserTest` to ensure the new field is null-tolerant
- [ ] Update `UserPersistenceMapper` to map `especialidadId` in both directions
- [ ] Update `UserRepository.findByEmail(...)` to use the new `User.withRoleAndSpecialty` graph (replacing `User.withRole`) so callers receive the specialty in the same load
- [ ] Update `UserResponse` DTO: add `UUID especialidadId` and `String especialidadNombre` (nullable)
- [ ] Update `UserRestMapper.toResponse(...)` to populate both fields (read `especialidadNombre` from the joined specialty entity via the graph; null when `especialidadId` is null)
- [ ] Update `CreateUserRequest`: add optional `UUID especialidadId` (no `@NotNull`)
- [ ] Update `CreateAdminUserServiceTest` to cover: caller provides a valid `especialidadId` → user persisted with it; caller provides an unknown `especialidadId` → `SpecialtyNotFoundException` before Firebase creation; caller omits `especialidadId` → user persisted with `especialidadId = null` → **RED**
- [ ] Update `CreateAdminUserService`: inject `GetSpecialtyByIdUseCase`; if `especialidadId != null`, call it before `createFirebaseUser`; pass the resolved id into `User.create(...)` → **GREEN**
- [ ] Update `UserResourceIT` (admin create flow) to assert the new fields appear in `201` responses

## Cross-feature impact — `patient-context` feature

- [ ] Update `PacienteContextoEntity` (table `paciente_contexto`): add scalar `UUID especialidadId` column mapped to `especialidad_id` (no `@ManyToOne`, mirroring how `paciente_id` is stored as scalar)
- [ ] Update `PacienteContexto` domain model: add nullable `UUID especialidadId`; update constructor and any `with*` builders; update `PacienteContextoTest` to allow null
- [ ] Update `PacienteContextoPersistenceMapper` to map `especialidadId`
- [ ] Update `PacienteContextoResponse` DTO: add `UUID especialidadId` (nullable)
- [ ] Update `PacienteContextoRestMapper.toResponse(...)` to include it
- [ ] Update `AddPacienteContextoServiceTest` to cover: caller has a specialty → entry persisted with that specialty; caller has no specialty (null) → entry persisted with `especialidadId = null` → **RED**
- [ ] Update `AddPacienteContextoService`: read `caller.user.especialidadId` from `AuthenticatedUserContext` (already injected) and pass it to the domain constructor → **GREEN**
- [ ] Update `PacienteContextoResourceIT` to assert the response includes `especialidadId` and that it equals the caller's specialty

## Cross-feature impact — `article` feature

- [ ] Update `ArticleEntity` (table `articulos_cientificos`): add `@ManyToOne(fetch = LAZY)` to `SpecialtyEntity` on `especialidad_id` (`insertable=false, updatable=false`) **and** a scalar `UUID especialidadId` column for write paths
- [ ] Add `@NamedEntityGraph("Article.withTagsAndSpecialty")` (covers tag + specialty fetch for the matching query in feature 3); keep the existing `Article.withTags` for endpoints that do not need specialty
- [ ] Update `Article` domain model: add nullable `UUID especialidadId`; update constructor; update `Article.create(...)` to default it to null; ensure `ArticleTest` covers the null case
- [ ] Update `ArticlePersistenceMapper` to map `especialidadId`
- [ ] Update `ArticleResponse` DTO: add `UUID especialidadId` and `String especialidadNombre` (nullable)
- [ ] Update `ArticleRestMapper.toResponse(...)` to populate both (read `especialidadNombre` via the new graph)
- [ ] **Confirm** (no change) that `CreateArticleRequest` and `POST /api/articles` do **not** accept `especialidadId` — manual creation leaves it null; AI auto-tagging in feature 3 will set it
- [ ] Update existing `ArticleRepositoryImplTest` to verify the new column round-trips (insert with null, insert with id, fetch via graph)
- [ ] Update existing `ArticleResourceIT` to assert the new response fields are present (and null) on the manual-create path

## Verification

- [ ] `./mvnw compile` clean (no Hibernate `validate` errors at boot)
- [ ] `./mvnw test` all green
- [ ] `./mvnw verify -DskipITs=false` all green
- [ ] Coverage ≥ 80% on `domain/specialty` and `application/specialty`
- [ ] Coverage on the modified `user`, `pacientecontexto`, and `article` packages did not regress
- [ ] Manual smoke test via Swagger UI:
  - `GET /api/especialidades` returns 16 rows
  - `GET /api/especialidades/00000000-0000-1000-8000-000000000005` returns "Pediatría"
  - `POST /api/admin/especialidades` as non-COO returns 403
  - Re-running `quarkus:dev` against an existing DB applies no migrations (V9–V11 already there) and the seed update is a no-op

## Wrap-up

- [ ] **Do not** write `summary.md` in this session — it is filled post-implementation in a future session per [specs/README.md](../../README.md#spec-lifecycle).
