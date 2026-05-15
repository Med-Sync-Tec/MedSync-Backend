# Feature: Medical Vocabulary — Requirements

## Overview

A **per-specialty controlled vocabulary** of clinical terms (~200 per specialty) used to constrain AI tag extraction in features 3 (`article-ai-analysis`) and 4 (`consulta-ai-analysis`). The vocabulary is **not stored in the database**: it lives as JSON resources in `src/main/resources/vocabulary/<especialidad-slug>.json`, version-controlled with the codebase, loaded at application startup into an immutable in-memory structure, and queried by domain port `VocabularyRepository.getVocabularyFor(especialidadId)`.

This feature also exposes a COO-only debug endpoint (`GET /api/especialidades/{id}/vocabulary`) so operators can audit exactly what the AI is allowed to extract for each specialty.

## User stories

- As the **AI flow** (features 3 and 4), I want a fast, in-memory lookup of "what terms am I allowed to extract for specialty X?", so that I can constrain the LLM output without paying per-call I/O.
- As a **COO**, I want to inspect a specialty's vocabulary, so that I can audit clinical coverage and explain to clinicians what the AI is allowed to recognize.
- As a **clinical content editor (developer or COO via PR)**, I want vocabulary changes to live in version control as reviewable JSON diffs, so that every change is auditable and tied to a release.
- As an **operator**, I want a malformed vocabulary file to fail the application boot, so that a typo never silently degrades AI extraction in production.

## Acceptance criteria (EARS format)

### Ubiquitous

- The system shall locate per-specialty vocabularies as classpath resources under `src/main/resources/vocabulary/`, one JSON file per active specialty, named `<slug>.json` (matching the specialty's `slug` field from feature 1).
- The system shall load all vocabulary files **once at application startup** into an immutable, thread-safe in-memory structure.
- The system shall expose lookup via a domain port `VocabularyRepository.getVocabularyFor(UUID especialidadId)` returning a `Vocabulary` aggregate (possibly empty) — never `null`.
- The system shall classify each vocabulary term by `TipoClinico` (`ENFERMEDAD`, `SINTOMA`, `TRATAMIENTO`, `MEDICAMENTO`), the shared enum already used by `article` and `patient-context`.
- The system shall validate each term: 1..500 chars, non-blank, no duplicates within the same `(especialidad, tipo)` bucket (case-insensitive).

### Event-driven

- When the application starts, the system shall:
  - Enumerate every `*.json` file under `vocabulary/` on the classpath.
  - Parse each file against the published JSON schema (see `design.md`).
  - For every parsed file, look up the specialty by its `slug` (filename and the JSON's `especialidadSlug` field — both must match each other).
  - Populate the in-memory structure with `(UUID especialidadId, Map<TipoClinico, List<VocabularyTerm>>)` entries.
- When a `GET /api/especialidades/{id}/vocabulary` request arrives from a **COO**, the system shall return `200 OK` with the loaded vocabulary for that specialty (empty `termsByType` if none was loaded for that id), including a count of total terms.

### State-driven

- While the application is running, the in-memory vocabulary is **read-only** — there is no runtime mutation endpoint. Updates require a code change and a redeploy.
- While a specialty has no corresponding JSON file (or its file failed the soft-skip path documented below), `VocabularyRepository.getVocabularyFor(...)` shall return an **empty** `Vocabulary` rather than throwing.

### Conditional (fail-fast at startup)

- If any JSON file under `vocabulary/` fails to parse, the system shall **abort startup** with a log entry naming the offending filename and the parse error.
- If a JSON file's `especialidadSlug` field does not match its filename (without `.json`), the system shall **abort startup** with a clear log entry.
- If a JSON file declares a term key under `terms` that is not a valid `TipoClinico` enum constant, the system shall **abort startup** naming the offending key.
- If a JSON file declares a term that exceeds 500 chars or is blank, the system shall **abort startup** naming the offending term.
- If a JSON file declares duplicate terms within the same `(specialty, tipo)` bucket (case-insensitive), the system shall **abort startup** naming the duplicates.

### Conditional (soft-skip at startup, log a warning)

- If a JSON file's `especialidadSlug` does not correspond to any specialty in the database, the system shall **log a warning** and skip that file (typically a stale file for a soft-deleted specialty).
- If an active specialty in the database has no corresponding JSON file in `vocabulary/`, the system shall log an informational message and continue. `getVocabularyFor(...)` for that specialty returns an empty `Vocabulary` — AI features handle this gracefully (see features 3 and 4).

### Conditional (REST)

- If a non-COO authenticated user calls `GET /api/especialidades/{id}/vocabulary`, the system shall return `403 Forbidden`.
- If an unauthenticated request reaches the endpoint, the system shall return `401 Unauthorized`.
- If the `{id}` does not match an existing specialty (active or soft-deleted), the system shall return `404 Not Found`.

## Non-functional requirements

- **Lookup performance**: `getVocabularyFor(...)` returns in O(1) (`HashMap` keyed by `UUID`) and never performs I/O after startup.
- **Memory footprint**: 16 specialties × ~200 terms × ~80 bytes/term ≈ 250 KB heap — acceptable.
- **Boot-time correctness > runtime tolerance**: malformed vocabulary is a developer/operator mistake. Failing fast at boot guarantees production never runs with silently degraded AI extraction.
- **Compatibility**: the loader is Quarkus-agnostic in `domain/`; the implementation in `infrastructure/` may use any classpath-scanning helper available in Quarkus.
- **Reproducibility**: vocabulary files are committed to git. A `git blame` on any line answers "when and why was this term added?".

## Out of scope (explicit)

- **Database persistence** of the vocabulary. The vocabulary is intentionally a code-managed artifact.
- **Runtime mutation** of the vocabulary (no `POST` / `PUT` / `DELETE` endpoints).
- **Hot reload** of vocabulary files without a restart. Operators redeploy to pick up new vocabularies.
- **Synonyms / aliases / multilingual mapping**. Each term has a single canonical Spanish surface form. Synonym handling is deferred to a future spec.
- **Term provenance** (who added the term, when, source citation). Versioning lives at the JSON file level (one `version` field per file) and in `git log`.
- **Vocabulary editing UI**. No frontend for editing terms — it is a developer / COO PR workflow.
- **Backfill / migration** of vocabulary terms to a future database table. If we move to DB-backed vocabularies, that is a separate spec.

## Open questions

- Should we enforce a maximum term count per specialty? *Not blocking — current scale is ~200 per specialty, well below any reasonable limit. If memory becomes a concern, revisit.*
- Should the soft-skip case for "JSON exists but specialty does not" be promoted to a fail-fast? *Not blocking — the soft-skip is intentional for the workflow where a developer prepares a vocabulary file before the COO creates the specialty entry.*
