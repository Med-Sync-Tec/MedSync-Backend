# Feature: Medical Vocabulary — Implementation Tasks

Execute in order. Each item ≈ one commit. Use TDD per [conventions/testing.md](../../../conventions/testing.md).

Depends on feature 1 (`specialty`) being implemented and merged. The loader queries `SpecialtyRepository` and the debug-endpoint service depends on `GetSpecialtyByIdUseCase`.

The operator runs all test commands; the agent writes the tests and waits.

## Domain

- [ ] Write `VocabularyTermTest` (unit, pure JUnit) covering: `tipo` null → `InvalidVocabularyTermDataException`; `valor` null / blank / whitespace-only / > 500 chars → exception; happy path retains the original (non-trimmed) `valor` exactly as supplied → **RED**
- [ ] Implement `domain/vocabulary/model/VocabularyTerm.java` + `domain/vocabulary/exception/InvalidVocabularyTermDataException.java` → **GREEN**
- [ ] Write `VocabularyTest` (unit, pure JUnit) covering: constructor normalizes the `termsByType` map so all four `TipoClinico` keys are present (missing → `List.of()`); `Vocabulary.empty(id, slug)` returns version `"empty"` with all empty buckets; `totalTerms()` sums correctly; `getTermsFor(tipo)` returns the right bucket; `containsTerm(tipo, valor)` is case-insensitive and trim-tolerant; `allValoresFor(tipo)` returns a `Set<String>` of canonical (non-lowercased) values; defensive copies — mutating an input list/map after construction does not affect the aggregate → **RED**
- [ ] Implement `domain/vocabulary/model/Vocabulary.java` → **GREEN**
- [ ] Define `domain/vocabulary/repository/VocabularyRepository.java` (single method `getVocabularyFor(UUID)`)
- [ ] Define `domain/vocabulary/usecase/GetVocabularyByEspecialidadUseCase.java`
- [ ] Add `domain/vocabulary/exception/VocabularyParseException.java` (constructor takes filename + offending field/term + cause)

## Application

- [ ] Write `GetVocabularyByEspecialidadServiceTest` (Mockito) covering: unknown specialty id → `SpecialtyNotFoundException` (via the mocked `GetSpecialtyByIdUseCase`); known id with loaded vocabulary → returns it; known id with no loaded vocabulary → returns `Vocabulary.empty(...)` → **RED**
- [ ] Implement `application/vocabulary/GetVocabularyByEspecialidadService.java` → **GREEN**

## Infrastructure — JSON parser

- [ ] Write `VocabularyJsonReaderTest` (unit, pure JUnit, fixtures inlined as `String` literals) covering:
  - Happy path: valid JSON → `ParsedVocabularyFile` with all four buckets populated.
  - Missing `especialidadSlug` → `VocabularyParseException` naming the field.
  - `especialidadSlug` ≠ filename slug → exception naming both.
  - Missing `version` → exception.
  - Missing `terms` → exception.
  - Unknown `terms` key (e.g. `"ALERGIA"`) → exception naming the key.
  - Empty / whitespace-only term → exception naming the bucket and position.
  - Term > 500 chars → exception.
  - Case-insensitive duplicates within `(specialty, tipo)` → exception listing both duplicates.
  - Empty bucket (`"TRATAMIENTO": []`) → no exception, bucket is empty.
  - Omitted bucket (no `"TRATAMIENTO"` key at all) → no exception, bucket is empty.
  - Malformed JSON (truncated braces) → `VocabularyParseException` wrapping Jackson's error.
  - `$schema` value other than `"medsync.vocabulary.v1"` → exception.
  → **RED**
- [ ] Implement `infrastructure/vocabulary/VocabularyJsonReader.java` (stateless, uses Jackson; exposes `ParsedVocabularyFile parse(String expectedSlug, byte[] jsonBytes) throws VocabularyParseException`) → **GREEN**
- [ ] Implement `infrastructure/vocabulary/ParsedVocabularyFile.java` (record `(String slug, String version, Map<TipoClinico, List<String>> termsByType)`)

## Infrastructure — loader + repository impl

- [ ] Write a `VocabularyRepositoryImplLoaderTest` that drives the loader's **pure logic** (no Quarkus boot) via a refactored seam: the loader exposes a package-private `build(Map<String, UUID> slugToId, List<RawFile> files)` that takes already-read file bytes. The test feeds:
  - Three files, all valid → returns `Map<UUID, Vocabulary>` keyed correctly, with the right total term counts.
  - One file whose slug has no matching specialty → result excludes it; a warning was emitted (capture via a stub logger).
  - One specialty with no file → not in the result map; `getVocabularyFor(thatId)` (after `installMap`) returns `Vocabulary.empty(...)`.
  - One malformed file → `VocabularyParseException` propagates out of `build(...)`.
  → **RED**
- [ ] Implement `infrastructure/vocabulary/VocabularyRepositoryImpl.java`:
  - `@ApplicationScoped` implements `VocabularyRepository`.
  - Holds a `volatile Map<UUID, Vocabulary> bySpecialtyId` (initialized to `Map.of()` so reads before `@Startup` finishes return empty rather than NPE).
  - `installMap(Map<UUID, Vocabulary>)` — package-private, called by the loader at startup; wraps with `Map.copyOf(...)`.
  - `getVocabularyFor(UUID)` reads the map and falls back to `Vocabulary.empty(...)`.
  → **GREEN**
- [ ] Implement `infrastructure/vocabulary/VocabularyLoader.java`:
  - `@ApplicationScoped`.
  - Observes `StartupEvent` (`void onStart(@Observes StartupEvent ev)`).
  - Injects `SpecialtyRepository`, `VocabularyJsonReader`, `VocabularyRepositoryImpl`.
  - Calls `ClassPathUtils.consumeAsPaths("vocabulary", consumer)` to enumerate files.
  - Builds the map via the refactored `build(...)` seam, then `installMap`.
  - Logs counts at INFO level: "Loaded vocabulary for N specialties (X terms total). M specialties have no vocabulary file. K stale files were skipped."
  - On `VocabularyParseException`, rethrows — Quarkus aborts boot.
- [ ] Add `quarkus.native.resources.includes=vocabulary/*.json` to `application.properties` (preparation for a future native build; harmless in JVM mode)

## REST

- [ ] Write `VocabularyResourceIT` (`@QuarkusTest` + RestAssured) covering:
  - 401 when unauthenticated.
  - 403 when authenticated as a non-COO.
  - 404 when the specialty id is unknown.
  - 200 when authenticated as COO and the specialty has a loaded vocabulary — response contains all four `TipoClinico` keys, the loaded version string, and the correct `totalTerms` count.
  - 200 when authenticated as COO and the specialty has **no** loaded vocabulary — response has `version: "empty"`, all four buckets are `[]`, `totalTerms: 0`.
  → **RED**
- [ ] Implement `interfaces/rest/vocabulary/VocabularyResponse.java` (record per `design.md`)
- [ ] Implement `interfaces/rest/vocabulary/VocabularyRestMapper.java` (`toResponse(Vocabulary)` — sorts each bucket alphabetically for stable consumer parsing)
- [ ] Implement `interfaces/rest/vocabulary/VocabularyResource.java`:
  - `@Path("/api/especialidades/{id}/vocabulary")`
  - One method, `GET`, requires COO via manual check on `AuthenticatedUserContext`.
  - Delegates to `GetVocabularyByEspecialidadUseCase`.
  - `@Tag("Admin")`, `@APIResponse` annotations for 200 / 401 / 403 / 404.
  → **GREEN**
- [ ] Wire `InvalidVocabularyTermDataException → 400 INVALID_VOCABULARY_TERM_DATA` into `infrastructure/config/GlobalExceptionHandler` (defensive — currently only thrown at startup, but the mapping is documented for future-proofing). `VocabularyParseException` is **not** wired (it only fires at boot, before the handler exists).
- [ ] Verify Swagger UI lists the new endpoint under `Admin`.

## Vocabulary fixture files

- [ ] Create `src/main/resources/vocabulary/cardiologia.json` populated with **50–80 representative terms** distributed across all four `TipoClinico` buckets. Use canonical Spanish forms with accents. `version: "2026-05-12"`.
- [ ] Create the 15 stub files (one per remaining seeded specialty: `endocrinologia.json`, `neurologia.json`, `oncologia.json`, `pediatria.json`, `gastroenterologia.json`, `neumologia.json`, `dermatologia.json`, `ginecologia.json`, `urologia.json`, `psiquiatria.json`, `reumatologia.json`, `oftalmologia.json`, `hematologia.json`, `medicina-interna.json`, `infectologia.json`). Each contains exactly 4 placeholder terms (one per `TipoClinico` bucket) with `version: "2026-05-12-stub"`. Mark each file with a top-of-file comment-equivalent (since JSON has no comments, prefix the `$schema` value or `version` value with `stub` so it is greppable) — using `version: "2026-05-12-stub"` is sufficient.
- [ ] Add a CI / `README` note (not a runtime check) listing the stub files so subsequent PRs can find them: append a short paragraph to `docs/specs/medical-vocabulary/12-05-2026/design.md`'s "Vocabulary file fixtures" section if not already there. (Already present — verify the list is accurate before merging.)

## Verification

- [ ] `./mvnw compile` clean
- [ ] `./mvnw test` all green
- [ ] `./mvnw verify -DskipITs=false` all green
- [ ] Coverage ≥ 80% on `domain/vocabulary` and `application/vocabulary`
- [ ] Boot-time smoke test: start `./mvnw quarkus:dev` and confirm the INFO log line:
  - "Loaded vocabulary for 16 specialties (N terms total). 0 specialties have no vocabulary file. 0 stale files were skipped."
- [ ] Negative boot-time smoke test (manual, do **not** commit): rename `cardiologia.json` to `cardiologiaX.json` (slug mismatch) and confirm `./mvnw quarkus:dev` aborts with a `VocabularyParseException` naming the file. Restore the filename before continuing.
- [ ] Manual Swagger UI smoke: `GET /api/especialidades/{id}/vocabulary` for the cardiología id (`00000000-0000-1000-8000-000000000001`) returns the full populated vocabulary; for any stub specialty, returns the 4-term stub vocabulary.

## Wrap-up

- [ ] **Do not** write `summary.md` in this session — filled post-implementation in a future session.
