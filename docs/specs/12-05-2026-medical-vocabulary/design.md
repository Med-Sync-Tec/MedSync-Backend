# Feature: Medical Vocabulary — Design

> Spec name: `medical-vocabulary`. Java package: `vocabulary` (single-noun, English from day one). The domain port is named `VocabularyRepository`, not `MedicalVocabularyRepository`, to match the dominant noun in the codebase. This is a deliberate spec-name ↔ package asymmetry — analogous to (but cleaner than) the legacy `patient-context` ↔ `pacientecontexto` case in [conventions/naming.md](../../conventions/naming.md#legacy-spanish-packages).

## Domain model

### `Vocabulary` (immutable aggregate, `domain/vocabulary/model/Vocabulary.java`)

| Field             | Type                                       | Required | Notes                                                              |
|-------------------|--------------------------------------------|----------|--------------------------------------------------------------------|
| especialidadId    | `UUID`                                     | yes      | id of the owning specialty (from feature 1)                        |
| especialidadSlug  | `String`                                   | yes      | redundantly carried for diagnostics; matches `Specialty.slug`      |
| version           | `String`                                   | yes      | free text — recommended: ISO date `YYYY-MM-DD` or semver           |
| termsByType       | `Map<TipoClinico, List<VocabularyTerm>>`   | yes      | always non-null; missing `TipoClinico` keys map to empty lists     |

Domain rules:

- The map is **defensively copied** in the constructor: keys are guaranteed to be the full set of `TipoClinico` values (missing buckets are normalized to empty `List.of()`). Values are wrapped with `List.copyOf(...)`.
- The aggregate is fully immutable — no setters, no mutators.

Behaviors:

- `Vocabulary.empty(UUID especialidadId, String slug)` — factory returning a `Vocabulary` with `version = "empty"` and every bucket as `List.of()`. Used when a specialty has no JSON file.
- `int totalTerms()` — sum of all bucket sizes.
- `List<VocabularyTerm> getTermsFor(TipoClinico tipo)` — returns the (possibly empty) list for that type.
- `boolean containsTerm(TipoClinico tipo, String valor)` — case-insensitive, trimmed equality on `valor` within the given bucket. Used by features 3 and 4 to validate that an LLM-extracted tag belongs to the constrained set.
- `Set<String> allValoresFor(TipoClinico tipo)` — flat set of the canonical `valor` strings for the given type, suitable for inclusion in an LLM prompt.

### `VocabularyTerm` (value object)

| Field       | Type           | Required | Notes                          |
|-------------|----------------|----------|--------------------------------|
| tipo        | `TipoClinico`  | yes      | shared enum                    |
| valor       | `String`       | yes      | 1..500 chars, trimmed, non-blank |

Invariants in constructor:

- `tipo` not null.
- `valor` not null, trimmed-non-blank, length ≤ 500.

No `id` field — vocabulary terms are intrinsically identified by `(tipo, valor)` and are not persisted as rows.

## Output port

```java
public interface VocabularyRepository {
    Vocabulary getVocabularyFor(UUID especialidadId);   // never null; empty Vocabulary if no file loaded
}
```

One method. The factory `Vocabulary.empty(...)` lives on the domain model, not on the port.

`VocabularyRepository` is a `*Repository` (own data — the JSON resources are owned by this codebase, committed to git, deployed as part of the application artifact). It is **not** a `*Gateway` despite being I/O-backed at startup, because [ADR 0002](../../architecture/decisions/0002-gateway-vs-repository.md) distinguishes by *ownership*, not by storage medium. We own these files.

## Use cases

| Use case interface                          | Service                                       | Trigger / endpoint                                   |
|---------------------------------------------|-----------------------------------------------|------------------------------------------------------|
| `GetVocabularyByEspecialidadUseCase`        | `GetVocabularyByEspecialidadService`          | `GET /api/especialidades/{id}/vocabulary` (debug)    |

The service injects `GetSpecialtyByIdUseCase` (from feature 1) to validate that the `especialidadId` references an existing specialty (throws `SpecialtyNotFoundException` on miss); then calls `VocabularyRepository.getVocabularyFor(...)` and returns the result.

Features 3 and 4 inject `VocabularyRepository` **directly** — they do not go through the use case because they orchestrate richer flows (LLM call + tag persistence) and the vocabulary lookup is one of several steps. Going through a use case would add a layer with no behavior of its own.

## REST endpoints

| Method | Path                                          | Request DTO | Response DTO         | Status codes                |
|--------|-----------------------------------------------|-------------|----------------------|-----------------------------|
| GET    | `/api/especialidades/{id}/vocabulary`         | —           | `VocabularyResponse` | 200 / 401 / 403 / 404       |

Swagger tag: `Admin`. The endpoint is COO-only — same manual-check pattern as the rest of the admin surface (`caller.roleName().equalsIgnoreCase("COO")`).

The path nests under `/api/especialidades/{id}` to make the relationship visible in OpenAPI without coupling the controllers: the resource class lives in `interfaces/rest/vocabulary/`, owns the `@Path("/api/especialidades/{id}/vocabulary")` declaration, and is independent of `SpecialtyResource`.

### DTOs (`interfaces/rest/vocabulary/`)

- `VocabularyResponse` — record:
  ```
  (UUID especialidadId,
   String especialidadSlug,
   String version,
   Map<String, List<String>> termsByType,    // keys are TipoClinico.name(); values sorted alphabetically
   int totalTerms)
  ```
  `termsByType` always includes all four `TipoClinico` keys (`"ENFERMEDAD"`, `"SINTOMA"`, `"TRATAMIENTO"`, `"MEDICAMENTO"`); missing-bucket cases serialize as empty arrays for predictable consumer parsing.

- `VocabularyTermResponse` (not used as a top-level type — terms are flattened to `String` arrays under each TipoClinico key for readability of the JSON dump).

## JSON file schema — `src/main/resources/vocabulary/<slug>.json`

```json
{
  "$schema": "medsync.vocabulary.v1",
  "especialidadSlug": "cardiologia",
  "version": "2026-05-12",
  "terms": {
    "ENFERMEDAD": [
      "Insuficiencia cardíaca congestiva",
      "Hipertensión arterial",
      "Infarto agudo de miocardio"
    ],
    "SINTOMA": [
      "Disnea",
      "Dolor torácico",
      "Palpitaciones"
    ],
    "TRATAMIENTO": [
      "Cateterismo cardíaco",
      "Angioplastia coronaria"
    ],
    "MEDICAMENTO": [
      "Losartán",
      "Atenolol",
      "Atorvastatina"
    ]
  }
}
```

### Schema rules (enforced at load time)

| Field              | Rule                                                                                                                             |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `$schema`          | Optional today. If present, must equal `"medsync.vocabulary.v1"`. Forward-compatible escape hatch for future schema migrations.   |
| `especialidadSlug` | Required, non-blank, must match the filename without `.json`.                                                                    |
| `version`          | Required, non-blank, ≤ 50 chars. Recommended: ISO date or semver. Stored verbatim and surfaced in the debug endpoint.            |
| `terms`            | Required object. Keys must be from `{ENFERMEDAD, SINTOMA, TRATAMIENTO, MEDICAMENTO}`. Unknown keys → **fail-fast**.               |
| Each term          | Non-blank string, trimmed length 1..500. Whitespace-only or empty → fail-fast. > 500 chars → fail-fast.                          |
| Term duplicates    | Within `(specialty, tipo)`, case-insensitive trimmed comparison. Duplicates → fail-fast naming the duplicates.                   |
| Empty bucket       | Allowed. A bucket may be `[]` or simply omitted.                                                                                  |

The `$schema` field is for human / tooling consumption (JSON-language-server pointer); the loader does not consult an external schema definition file.

## In-memory layout

```java
@ApplicationScoped
public class VocabularyRepositoryImpl implements VocabularyRepository {

    private final Map<UUID, Vocabulary> bySpecialtyId;   // populated once in @Startup, never mutated

    @Override
    public Vocabulary getVocabularyFor(UUID especialidadId) {
        return bySpecialtyId.getOrDefault(
            especialidadId,
            Vocabulary.empty(especialidadId, "unknown")
        );
    }
}
```

The map is built once at startup, wrapped in `Map.copyOf(...)` for unmodifiability, and assigned to the `final` field. All reads are lock-free.

## Loading strategy

A single `@Startup` component owns the boot-time pipeline. Implementation lives in `infrastructure/vocabulary/`.

### `VocabularyLoader` (`@ApplicationScoped`, `@Startup`)

Pseudo-flow on application boot:

```
1. specialties ← SpecialtyRepository.findAllActive()     // List<Specialty>
2. slugToId     ← specialties.toMap(Specialty::slug, Specialty::id)
3. files        ← ClassPathUtils.consumeAsPaths("vocabulary", p -> list .json files)
4. for each file in files:
     slug   = file.fileName().stripSuffix(".json")
     bytes  = read(file)
     parsed = VocabularyJsonReader.parse(slug, bytes)    // throws on schema violation → boot aborts
     if slug not in slugToId:
         log.warn("Vocabulary file '<slug>.json' has no matching specialty in DB — skipping (possibly a stale file).")
         continue
     loaded[slugToId.get(slug)] = Vocabulary.from(parsed, slugToId.get(slug))
5. for each specialty in specialties:
     if specialty.id not in loaded:
         log.info("No vocabulary file for specialty '<slug>' — AI features will see an empty vocabulary.")
6. VocabularyRepositoryImpl.installMap(Map.copyOf(loaded))   // package-private setter; one-shot
```

The classpath enumeration uses Quarkus's `io.quarkus.runtime.util.ClassPathUtils.consumeAsPaths("vocabulary", consumer)` which works uniformly in dev mode (filesystem under `target/classes/vocabulary/`) and packaged JAR mode (resources inside the jar). We isolate this dependency in `infrastructure/vocabulary/` so it never leaks into `domain/` or `application/`.

If `ClassPathUtils.consumeAsPaths` throws (truly catastrophic — classpath unreadable), the exception propagates and Quarkus startup fails. That is the right behavior.

### `VocabularyJsonReader` (`@ApplicationScoped`, but stateless utility class)

Pure parsing helper. Uses Jackson (already on the classpath via Quarkus REST + JSON):

```java
public ParsedVocabularyFile parse(String expectedSlug, byte[] jsonBytes) throws VocabularyParseException;
```

Returns a `ParsedVocabularyFile(slug, version, Map<TipoClinico, List<String>>)`. Validates every schema rule listed above. Throws `VocabularyParseException` with a clear message naming the file and the offending field/term.

### `VocabularyParseException`

Thrown only at startup. **Not mapped to HTTP** in `GlobalExceptionHandler` — by the time HTTP is up, parsing is done. Boot abort is enforced by letting the exception propagate out of `@Startup`.

## Validation summary

| Level     | Check                                                                              | Location                            |
|-----------|------------------------------------------------------------------------------------|-------------------------------------|
| Format    | Domain `valor` ≤ 500, non-blank                                                    | `VocabularyTerm` constructor        |
| Format    | `tipo` non-null `TipoClinico`                                                      | `VocabularyTerm` constructor        |
| Schema    | JSON parse + required fields + slug match + known TipoClinico keys                  | `VocabularyJsonReader`              |
| Schema    | Term length, blank check, in-bucket case-insensitive dedup                          | `VocabularyJsonReader`              |
| App-level | Specialty exists for the requested id (debug endpoint)                              | `GetVocabularyByEspecialidadService`|
| App-level | Caller is COO (debug endpoint)                                                      | `VocabularyResource`                |

## Exceptions

| Exception                          | Where thrown                                      | HTTP / lifecycle                                                |
|------------------------------------|---------------------------------------------------|-----------------------------------------------------------------|
| `VocabularyParseException`         | `VocabularyJsonReader` during `@Startup`           | **Aborts boot.** No HTTP mapping needed (request layer not up). |
| `InvalidVocabularyTermDataException` | `VocabularyTerm` constructor                     | 400 if ever surfaced through HTTP (currently only at startup).  |
| `SpecialtyNotFoundException` (reused) | `GetVocabularyByEspecialidadService`            | 404                                                             |
| `RoleMismatchException` (reused)   | `VocabularyResource` (COO check)                  | 403                                                             |

`InvalidVocabularyTermDataException` is wired into `GlobalExceptionHandler` for symmetry (future-proofing if a hot-reload or runtime-edit feature ever lands), but in this snapshot it can only fire at startup.

## Sequence flow

### Boot

```
Quarkus       VocabularyLoader        SpecialtyRepository    Classpath    JsonReader    VocabularyRepositoryImpl
   │ @Startup    │                          │                    │            │                  │
   │ ──────────► │                          │                    │            │                  │
   │             │ findAllActive() ────────►│                    │            │                  │
   │             │ ◄── List<Specialty> ─────│                    │            │                  │
   │             │ consumeAsPaths("vocabulary") ──────────────────►            │                  │
   │             │ ◄── files: [cardiologia.json, …]                            │                  │
   │             │ for each file: parse(slug, bytes) ─────────────────────────►│                  │
   │             │ ◄── ParsedVocabularyFile / throw ────────────────────────── │                  │
   │             │ build Map<UUID, Vocabulary>                                 │                  │
   │             │ installMap(immutable) ─────────────────────────────────────────────────────────►
   │             │ log loaded count                                            │                  │
   │ ◄───────────│ (or BootException propagates if parse failed)               │                  │
```

### Debug endpoint (request)

```
Client       VocabularyResource    GetVocabularyByEspecialidadService    GetSpecialtyByIdService    VocabularyRepositoryImpl
  │ GET /api/especialidades/{id}/vocabulary           │                          │                          │
  │ ──────────────────────►                            │                          │                          │
  │                  assertCallerIsCoo()              │                          │                          │
  │                  execute(id) ──────────────────────►                          │                          │
  │                                       getSpecialtyByIdService.execute(id) ──►│                          │
  │                                       ◄── Specialty (or NotFound) ──────────│                          │
  │                                       getVocabularyFor(id) ───────────────────────────────────────────►│
  │                                       ◄── Vocabulary (possibly empty) ───────────────────────────────  │
  │                  ◄── Vocabulary ──────│                          │                          │
  │ ◄── 200 + VocabularyResponse ──────── │                          │                          │
```

## Vocabulary file fixtures shipped in this spec

This spec **does not** require shipping clinically complete vocabularies. The task list (next file) ships:

- **One fully-populated exemplar**: `cardiologia.json` with ~50–80 representative terms across all four `TipoClinico` buckets. Enough to prove the loader scales without inflating the PR with 3,200 lines of clinical content.
- **Fifteen 4-term stubs**: one term per `TipoClinico` for each of the other seeded specialties. Each file looks like:
  ```json
  {
    "especialidadSlug": "endocrinologia",
    "version": "2026-05-12-stub",
    "terms": {
      "ENFERMEDAD":    ["Diabetes mellitus tipo 2"],
      "SINTOMA":       ["Polidipsia"],
      "TRATAMIENTO":   ["Terapia insulínica"],
      "MEDICAMENTO":   ["Metformina"]
    }
  }
  ```
  The `-stub` suffix in `version` makes it trivial to grep stubs and replace them with real content in follow-up PRs.

Full clinical content for the other 15 specialties is **deferred** to subsequent PRs (one per specialty, written by clinical content editors / COO). The loader, schema, debug endpoint, and AI flows in features 3 / 4 all work against stubs — they simply have fewer terms to match against.

## Cross-feature impact

None on existing features (1-05-2026 snapshots remain canonical).

This feature **depends on feature 1** (`specialty`): the loader queries `SpecialtyRepository.findAllActive()` and the debug endpoint validates `especialidadId` via `GetSpecialtyByIdUseCase`. Both dependencies are one-way — `vocabulary` imports from `specialty`, not vice versa.

> **Note.** The existing `specs/1-05-2026-<feature>/` spec remains the canonical record for unchanged behavior; modified behavior will be re-snapshotted as `specs/<implementation-date>-<feature>/` when this work ships.

## Key technical decisions

### 1. JSON resources, not a database table

Vocabulary is reference data that changes through clinical review, not through user action. Storing it in JSON gives us:

- **Reviewable diffs** in PRs (every term added is visible in `git diff`).
- **Atomic releases** — vocabulary and code ship together; no separate "seed the DB" step that can drift in stage vs prod.
- **Zero-cost lookups** at runtime (one in-memory map, no SQL).
- **Easy ports to other backends** if the AI strategy changes (the data is plain JSON, not locked in a schema).

The trade-off — runtime mutation requires a redeploy — is acceptable for an MVP and aligned with the safety profile of medical software (clinical content changes should pass code review).

### 2. Driven by `SpecialtyRepository`, not by directory enumeration

The loader maps `slug → UUID` by querying the database first, then visits filesystem resources. This means:

- A specialty without a JSON file is silently allowed (`getVocabularyFor` returns empty). This is the right behavior for runtime-created specialties.
- A JSON file without a matching DB specialty is **warn-skipped** at boot (probably a stale file).
- The loader is robust to either side being temporarily out of sync — neither blocks boot.

We chose this over "fail boot if directory and DB don't match" because operators may run the same JAR against multiple databases (dev / stage / prod) with slightly different specialty rosters during onboarding.

### 3. Fail-fast on schema violations

A vocabulary file with a typo (`"ENFERMADAD"` instead of `"ENFERMEDAD"`), a duplicate term, or an oversize term is a developer mistake that **must** be caught before production traffic hits the AI flow. Letting it through would silently degrade extraction quality, which in a pharmacovigilance context can cause clinical false-negatives. The 30 seconds of operator pain (boot fails, log shows the exact file and term, fix the diff, re-deploy) is cheap insurance.

### 4. Soft-skip on cross-reference mismatches

Conversely, "a file exists for a specialty that no longer exists" or "a specialty exists with no file yet" are operational states the system should tolerate. Both are warn-logged so the operator notices, but neither blocks boot.

### 5. `Vocabulary.empty(...)` instead of `Optional<Vocabulary>`

Returning `Vocabulary.empty(...)` instead of `Optional.empty()` simplifies every caller. AI flows in features 3 and 4 want to ask "how many terms can I extract here?" and an empty list answers that without an extra null-check layer. The (rare) case where the caller wants to differentiate "no file" from "file with no terms" can use `vocabulary.version().equals("empty")` — but no current consumer needs that.

### 6. Case-insensitive containment, case-sensitive storage

`containsTerm(tipo, valor)` lowercases both sides for matching. Stored canonical terms preserve their original capitalization (`"Insuficiencia cardíaca"`, not `"insuficiencia cardíaca"`) because the canonical form is what the LLM and the debug endpoint emit verbatim. The case-insensitive *check* lets us tolerate LLM output that capitalizes inconsistently.

### 7. `VocabularyRepository`, not `VocabularyGateway`

We own these JSON files. They are deployed inside our artifact. They are in our git history. By the [ADR 0002](../../architecture/decisions/0002-gateway-vs-repository.md) criterion (ownership, not storage medium), this is a `*Repository`. We do not need timeout / retry / circuit-breaker semantics because there is no network or external system in the loop.

### 8. No use case for the AI hot path

Features 3 and 4 inject `VocabularyRepository` directly rather than going through `GetVocabularyByEspecialidadUseCase`. Reasons:

- The AI services orchestrate a multi-step flow (specialty resolve → vocabulary load → LLM call → save). A use case named "Get vocabulary by specialty" carries no orchestration of its own — it would be a one-line passthrough.
- One-service-per-use-case is a [project rule](../../conventions/naming.md#class-names); adding a use case here just to satisfy the rule would create an empty layer.
- The use case **is** kept for the debug REST endpoint, where it does its job: validate the specialty exists with the right exception, then read. That is the use-case-shaped operation.

### 9. The loader injects `SpecialtyRepository`, not `GetSpecialtyByIdService`

At boot time we want a bulk listing (`findAllActive()`), not a single-by-id lookup. Using the use-case interface would force per-id calls or an awkward "bulk list" use case that exists only for the loader. The repository port is fine here — the loader is an `infrastructure/` component and may depend on the port directly.

### 10. `$schema` field as a forward-compatibility hatch

Today we hardcode `"medsync.vocabulary.v1"` as the recognized schema. If a future revision introduces breaking changes (e.g. structured term metadata, multilingual variants), files can declare `"medsync.vocabulary.v2"` and the loader can switch parsers based on the value. Without this field, a future revision would require a two-step migration. Including it now costs nothing.

## Open technical decisions / risks

- **Classpath enumeration in native mode.** Quarkus native compilation may require explicit `quarkus.native.resources.includes=vocabulary/*.json` in `application.properties`. We will add that property as part of the implementation tasks but cannot fully verify it until the project decides to ship a native image — flagged as a known risk for the day native builds are introduced.
- **Test coverage of fail-fast at startup.** `@QuarkusTest` cannot easily test "the application failed to start because of a bad vocabulary file" without per-test classloader isolation. We test `VocabularyJsonReader` exhaustively at the unit level and rely on integration tests with valid fixtures for the wiring. The fail-fast behavior in `VocabularyLoader` is tested by injecting a malformed fixture via a unit test on the loader's pure logic (factored to take an `InputStream` source, not a classpath path).
- **Stub vocabularies in production traffic.** Shipping 15 stub files means 15 specialties have only 4 terms each at first deploy. AI flows in features 3 and 4 will return near-empty tag suggestions for those specialties until clinical content is populated. We accept this so the feature is fully wired end-to-end on day one; the COO can prioritize which specialties to populate first.
