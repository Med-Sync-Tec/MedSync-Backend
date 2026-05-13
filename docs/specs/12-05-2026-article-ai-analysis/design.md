# Feature: Article AI Analysis — Design

> Spec name: `article-ai-analysis`. Java package: `articleaianalysis` (single segment, lowercase, matches the existing `pacientecontexto` pattern for compound spec names). Domain port: `AiAnalysisGateway`, which is **shared with feature 4** (`consulta-ai-analysis`) and therefore lives in `domain/shared/repository/`, not in this feature's package.

## Shared AI gateway (lives in `domain/shared/`)

Both AI features depend on a single port:

```java
// domain/shared/repository/AiAnalysisGateway.java
public interface AiAnalysisGateway {
    ArticleAnalysisResult analyzeArticle(ArticleAnalysisRequest request);
    ConsultaAnalysisResult analyzeConsultaText(ConsultaAnalysisRequest request);
}
```

Supporting types (all in `domain/shared/model/`):

```java
// Request to analyze an article — drives both classification and extraction in one call.
public record ArticleAnalysisRequest(
    String titulo,                        // optional input signal, may be blank
    String abstractText,                  // required, non-blank
    String keywords,                      // optional input signal, may be blank
    List<SpecialtyDescriptor> candidateSpecialties,
    Map<UUID, Vocabulary> vocabulariesById
) {}

public record SpecialtyDescriptor(UUID id, String nombre, String slug, String descripcion) {}

public record ArticleAnalysisResult(UUID especialidadId, List<ExtractedTag> tags) {}

public record ExtractedTag(TipoClinico tipo, String valor) {}

public record ConsultaAnalysisRequest(String consultaText, Vocabulary vocabulary) {}

public record ConsultaAnalysisResult(List<ExtractedTag> tags) {}
```

`AiAnalysisGateway` is a `*Gateway` (external system over HTTP) per [ADR 0002](../../architecture/decisions/0002-gateway-vs-repository.md). The implementation is `GroqAiAnalysisGateway` in `infrastructure/ai/`, shared by both features 3 and 4. The port name is provider-agnostic so a future migration to a different LLM vendor would not break domain or application layers — only the impl is swapped.

### Why one gateway with two methods, not three

The prompt mandates "single `AiAnalysisGateway` with two methods (`analyzeArticle`, `analyzeConsultaText`)". The article method internally orchestrates two HTTP calls to Groq (classify → extract); the consulta method makes one. Both call paths share authentication, error mapping, timeout policy, and observability. Splitting into three port methods would force callers to know about the classify/extract decomposition that is purely an implementation detail of the Groq-backed adapter.

## Domain model (article-side additions)

### `Article` (updated)

The `Article` aggregate (defined in [specs/1-05-2026-article/design.md](../1-05-2026-article/design.md)) already has the fields needed. Two new behaviors land in this spec:

- `Article.withAiAnalysis(UUID especialidadId, List<ArticleTag> newTags)` — returns a new instance with `especialidadId` set and the tag list **replaced**.
  - The existing `cascade = ALL, orphanRemoval = true` configuration on `@OneToMany` ensures that on save, the old `ArticleTagEntity` rows are deleted and the new ones inserted in one transaction.
- The constructor invariants are unchanged. `especialidadId` is already nullable from feature 1's cross-feature impact.

### `ArticleTag` factory used here

`ArticleTag.create(tipo, valor)` — already exists from the article spec — is reused. The AI flow constructs fresh tags from the LLM output; the existing `id = UUID.randomUUID()` and `createdAt = Hibernate-assigned` pattern applies.

### Domain exceptions (new, `domain/articleaianalysis/exception/`)

| Exception                       | HTTP | When                                              |
|---------------------------------|------|---------------------------------------------------|
| `AiAnalysisException`           | 502  | Groq returns garbage / unknown specialty id / no valid tags after vocab filter |
| `AiAnalysisTimeoutException`    | 504  | The configured timeout elapses before the API responds |
| `AiConfigurationException`      | (boot abort) | API key missing or blank at startup       |

`AiConfigurationException` is thrown by the gateway at `@Startup` and aborts boot. It is **not** mapped to HTTP because by the time HTTP serves traffic, configuration has already been validated.

## Output ports

This feature does not introduce a new `*Repository` — it reuses:

- `ArticleRepository` (feature: `article`) — `findByUuid`, `save`.
- `SpecialtyRepository` (feature: `specialty`) — `findAllActive`.
- `VocabularyRepository` (feature: `medical-vocabulary`) — `getVocabularyFor`.
- `AiAnalysisGateway` (this feature, shared) — `analyzeArticle`.

## Use cases

| Use case interface                  | Service                              | Trigger                                  |
|-------------------------------------|--------------------------------------|------------------------------------------|
| `AnalyzeArticleWithAiUseCase`       | `AnalyzeArticleWithAiService`        | `POST /api/articles/{id}/analyze`        |

`AnalyzeArticleWithAiService` injects: `ArticleRepository`, `SpecialtyRepository`, `VocabularyRepository`, `AiAnalysisGateway`. Annotated `@Transactional` since it both reads the article and writes its replaced tag list.

The existing `GetMatchingArticlesByPatientService` (feature: `article`) is **modified** here — see "Cross-feature impact" below — but no new use case is added for it.

## REST endpoint

| Method | Path                                | Request DTO | Response DTO              | Status codes                          |
|--------|-------------------------------------|-------------|---------------------------|---------------------------------------|
| POST   | `/api/articles/{id}/analyze`        | —           | `AnalyzeArticleResponse`  | 200 / 400 / 401 / 404 / 502 / 504     |

No request body — the article id is in the path and all input comes from the loaded article. Swagger tag: `Articles`.

The resource lives in `interfaces/rest/articleaianalysis/` to avoid bloating `ArticleResource` from feature `article`. Path namespace overlap is fine (JAX-RS resolves by full path).

### DTO

```
record AnalyzeArticleResponse(
    UUID articleId,
    UUID especialidadId,
    String especialidadNombre,
    List<AnalyzedTagResponse> tags,
    String modelUsed,
    int promptTokens,
    int completionTokens
)

record AnalyzedTagResponse(UUID id, String tipo, String valor)
```

The `modelUsed` / `promptTokens` / `completionTokens` fields are surface-level observability — they help the frontend show "Analyzed with llama-3.3-70b-versatile in 1.2k tokens" so the doctor has a sense of provenance. The gateway captures these from the Groq response envelope (`usage.prompt_tokens` + `usage.completion_tokens`) and propagates them through `ArticleAnalysisResult` (extended below) — `ArticleAnalysisResult` becomes:

```java
public record ArticleAnalysisResult(
    UUID especialidadId,
    List<ExtractedTag> tags,
    String modelUsed,
    int promptTokens,
    int completionTokens
) {}
```

`ConsultaAnalysisResult` carries the same observability triple.

## Groq HTTP integration

### Configuration (`application.properties`)

```properties
ai.groq.api-key=${GROQ_API_KEY:}
ai.groq.model=llama-3.3-70b-versatile
ai.groq.max-tokens=2048
ai.groq.timeout=30s
ai.groq.base-url=https://api.groq.com/openai/v1
```

The default `api-key` is empty — at `@Startup`, `GroqAiAnalysisGateway` checks for non-blank and throws `AiConfigurationException` if missing. This is the fail-fast behavior required.

`GROQ_API_KEY` is obtained from the free Groq Cloud console at <https://console.groq.com/keys> — no credit card required for the free tier used by this project.

### Transport

Use the JDK 21 `java.net.http.HttpClient` directly. Reasons:

- No SDK constraint per the prompt.
- `HttpClient` is built-in, supports per-request timeouts, and integrates cleanly with `CompletableFuture` for the read-timeout retry semantics (not needed for MVP).
- Quarkus REST Client would require an interface stub + reactive plumbing for a single endpoint — not worth the boilerplate.

### Request shape

Groq exposes the OpenAI-compatible Chat Completions API. Authentication is a standard Bearer token; the request and response shapes are OpenAI-style, **not** the Anthropic Messages shape — there is no `anthropic-version` header and no top-level `system` field.

```http
POST {base-url}/chat/completions
Authorization: Bearer {api-key}
Content-Type: application/json

{
  "model": "llama-3.3-70b-versatile",
  "max_tokens": 2048,
  "temperature": 0,
  "response_format": { "type": "json_object" },
  "messages": [
    { "role": "system", "content": "<system prompt>" },
    { "role": "user",   "content": "<payload>" }
  ]
}
```

The `response_format: { "type": "json_object" }` directive enables Groq's JSON mode, which forces the model to return a syntactically valid JSON object. This simplifies the gateway's parser — we still validate the *shape* of the JSON, but we no longer need a brace-balanced extractor to fish JSON out of prose.

Required prerequisite of JSON mode: the system prompt must instruct the model to respond with JSON; otherwise Groq returns an error. Our gateway prompts include "Respond with a single JSON object matching the schema below." for both calls.

### Two-step article flow inside `analyzeArticle`

The article method makes **two** sequential HTTP calls:

**Step 1 — Specialty classification.** Prompt asks the model to pick one specialty id from a JSON-formatted list of `{id, nombre, slug, descripcion}` candidates given the article's `titulo + abstractText + keywords`. Required response shape (enforced by JSON mode):

```json
{ "especialidadId": "00000000-0000-2000-8000-000000000001" }
```

**Step 2 — Tag extraction.** With the chosen specialty's `Vocabulary` (flattened into `Map<String tipoName, List<String valores>>`), the second prompt asks for `(tipo, valor)` extractions strictly drawn from the provided terms. Required response:

```json
{
  "tags": [
    { "tipo": "ENFERMEDAD", "valor": "Insuficiencia cardíaca congestiva" },
    { "tipo": "MEDICAMENTO", "valor": "Losartán" }
  ]
}
```

### Response validation

The gateway:

1. Asserts the HTTP status is `2xx`. Non-2xx → `AiAnalysisException` carrying the upstream status code in the message. A 429 (free-tier rate limit) maps the same way; an empty body or 5xx surfaces as 502.
2. Parses the Groq response envelope and extracts `choices[0].message.content`. With JSON mode, this string is itself a valid JSON document.
3. Parses the inner JSON via Jackson.
4. For step 1: asserts `especialidadId` is a valid UUID and is present in the input `candidateSpecialties`. Otherwise → `AiAnalysisException`.
5. For step 2: for each returned tag, asserts `tipo` is a valid `TipoClinico` and `vocabulary.containsTerm(tipo, valor)` is true. Unknown tags are **filtered out** (not fatal); a count is logged. If the filtered list is empty, → `AiAnalysisException` ("no valid tags after vocabulary filter").
6. Captures `usage.prompt_tokens` and `usage.completion_tokens` from the Groq response into the `ArticleAnalysisResult`.

Timeout enforcement: `HttpClient.newBuilder().connectTimeout(...)` plus per-request `HttpRequest.newBuilder().timeout(Duration.ofSeconds(...))`. On `HttpTimeoutException` → `AiAnalysisTimeoutException`.

### Token estimate (informational)

Per article: ~1k input tokens (abstract + 16 specialty descriptors) + ~500 output tokens for step 1; ~500 input tokens (vocabulary slice) + ~200 output tokens for step 2. Combined ~2.2k tokens. Groq's free tier at the time of writing allows ~12k tokens/min on `llama-3.3-70b-versatile`, so a single analysis fits comfortably within one minute's budget. The same budget is shared across all users of the same API key — multi-doctor concurrent demos may need to slow down.

## Validation

| Level     | Check                                                                       | Location                                                |
|-----------|-----------------------------------------------------------------------------|---------------------------------------------------------|
| App-level | Article exists                                                              | `AnalyzeArticleWithAiService`                           |
| App-level | Article has non-blank `abstractText`                                        | `AnalyzeArticleWithAiService` (throws `InvalidArticleDataException`) |
| App-level | Returned `especialidadId` belongs to active specialty set                   | `GroqAiAnalysisGateway`                                 |
| App-level | Each returned tag exists in the chosen specialty's vocabulary               | `GroqAiAnalysisGateway` (filters silently, logs count) |
| App-level | At least one tag survives the filter                                        | `GroqAiAnalysisGateway` (else `AiAnalysisException`)    |
| Config    | `ai.groq.api-key` non-blank                                                 | `GroqAiAnalysisGateway` `@Startup`                      |

## Exceptions

| Exception                       | HTTP | Notes                                                                  |
|---------------------------------|------|------------------------------------------------------------------------|
| `ArticleNotFoundException`      | 404  | Reused from `article` feature.                                         |
| `InvalidArticleDataException`   | 400  | Reused; new message "Article has no abstract — AI analysis requires non-empty abstractText". |
| `AiAnalysisException`           | 502  | New. Wraps Groq transport / parsing failures and free-tier 429s.       |
| `AiAnalysisTimeoutException`    | 504  | New. Distinct from generic 502 so monitoring can alert on it.          |
| `AiConfigurationException`      | (boot abort) | New. Thrown only at startup; not mapped to HTTP.               |

All three new exceptions are wired into `GlobalExceptionHandler`.

## Sequence flow

### Analyze article

```
Doctor       ArticleAiResource     AnalyzeArticleService     ArticleRepo   SpecRepo   VocabRepo   AiGateway                  Groq
  │ POST /api/articles/{id}/analyze         │                       │            │            │            │                          │
  │ ────────────────────────►                │                       │            │            │            │                          │
  │                  execute(id, caller) ───►                       │            │            │            │                          │
  │                                           findByUuid(id) ───────►                       │            │            │                          │
  │                                           ◄── Article (or 404) │                       │            │            │                          │
  │                                           assert abstractText non-blank (else 400)      │            │            │                          │
  │                                           findAllActive() ────────────────►                          │            │            │                          │
  │                                           ◄── List<Specialty> ────────────│                         │            │            │                          │
  │                                           for each: getVocabularyFor(s.id) ─────────────►            │            │            │                          │
  │                                           ◄── Map<UUID, Vocabulary> ────────────────────│            │            │            │                          │
  │                                           analyzeArticle(request) ───────────────────────────────────►            │            │                          │
  │                                                            POST /chat/completions (classify) ───────────────────►│                          │
  │                                                            ◄── { especialidadId } ────────────────────────────── │                          │
  │                                                            POST /chat/completions (extract using chosen vocab) ─►│                          │
  │                                                            ◄── { tags: [...] } ──────────────────────────────── │                          │
  │                                           ◄── ArticleAnalysisResult ───────────────────────────────│            │            │                          │
  │                                           article ← article.withAiAnalysis(esp, tags)              │            │            │                          │
  │                                           save(article) ─────────► (DELETE old tags + INSERT new + UPDATE FK)    │            │            │                          │
  │                                           ◄── persisted Article ─                                   │            │            │                          │
  │                  ◄── AnalyzeArticleResponse ────                  │                       │            │            │            │                          │
  │ ◄── 200 + body ──│                                                  │                       │            │            │            │                          │
```

Transaction boundary: the whole `execute(...)` method is `@Transactional`. The Groq HTTP calls happen **inside** the transaction. This means a slow Groq response holds a DB connection longer than ideal. Trade-off accepted: keeping HTTP outside the transaction would require splitting the flow into "analyze (no DB)" + "save (no AI)" with potential race conditions on re-invocation. At MVP scale (manual trigger, low concurrency), the simpler model wins. Groq is typically fast (1–3 s per call), which mitigates the concern in practice.

### Updated matching query (`GetMatchingArticlesByPatient`)

The existing JPQL/SQL becomes:

```sql
SELECT DISTINCT a.*
FROM articulos_cientificos a
JOIN articulo_tags t   ON t.articulo_id = a.id
JOIN paciente_contexto c
    ON c.tipo = t.tipo
   AND c.valor = t.valor
   AND c.especialidad_id = a.especialidad_id      -- NEW
WHERE c.paciente_id = :patientId
ORDER BY a.updated_at DESC
LIMIT :limit
```

The new `AND c.especialidad_id = a.especialidad_id` clause means **rows on either side with NULL `especialidad_id` are excluded** (SQL NULL ≠ NULL). This is the intended behavior — un-analyzed articles and un-tagged contexts are not part of the matching pipeline until they are explicitly enriched.

## Cross-feature impact

| Existing feature   | Affected artifact                            | Change                                                                                                                | Migration that lands it |
|--------------------|----------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|-------------------------|
| `article`          | `Article` domain model                       | Adds `withAiAnalysis(UUID, List<ArticleTag>)` behavior.                                                               | (code only)             |
| `article`          | `ArticleRepository`                          | No new methods. `save(Article)` already handles tag replacement via `cascade=ALL, orphanRemoval=true`.                | (no change)             |
| `article`          | `ArticleRepositoryImpl.findMatchingByPatientContext` | SQL gains `AND c.especialidad_id = a.especialidad_id` in the join.                                          | (code only)             |
| `article`          | `GetMatchingArticlesByPatientService`        | No code change — the service delegates to the repository; the SQL change happens in the impl.                          | (no change)             |
| `article`          | `ArticleResponse` DTO                        | **Already** updated by feature 1 to expose `especialidadId` + `especialidadNombre`. No further change in feature 3.    | (no change)             |
| `infrastructure/config` | `GlobalExceptionHandler`                 | Adds mappings for `AiAnalysisException → 502`, `AiAnalysisTimeoutException → 504`.                                    | (code only)             |
| `application.properties` | AI configuration block                 | Adds `ai.groq.*` keys with defaults; documents required env var `GROQ_API_KEY`.                                       | (code only)             |

> **Note.** The existing `specs/1-05-2026-article/` spec remains the canonical record for unchanged behavior; modified behavior will be re-snapshotted as `specs/<implementation-date>-article/` when this work ships.

No database migration is required for this feature — feature 1 already added `especialidad_id` to `articulos_cientificos`.

## Key technical decisions

### 1. Two-step LLM flow inside the gateway

Classifying and extracting in one prompt with all 16 vocabularies' worth of terms (~3,200 lines) would inflate every prompt with content that is irrelevant once the specialty is chosen (15 of 16 vocabularies). Splitting into two calls keeps each prompt under 2 KB of input, stays well inside the free-tier per-minute token budget, and lets us reuse `analyzeConsultaText`'s extraction logic conceptually for step 2.

### 2. Replace, not append, on every invocation

The endpoint is "I want the AI's best current answer on this article". Appending leads to duplicate tags over re-invocations; refusing to re-analyze requires a separate re-tag endpoint. Replacing is the cleanest semantic. The frontend warns the doctor before invoking ("This will replace existing tags. Continue?").

The doctor's recourse to fix mistakes is `DELETE /api/articles/{id}/tags/{tagId}` (existing endpoint from the article spec) — narrow, focused, idempotent.

### 3. Vocabulary filter is silent, not strict

If the LLM returns `{ "tipo": "ENFERMEDAD", "valor": "Frobozz syndrome" }` and `"Frobozz syndrome"` is not in the cardiología vocabulary, we drop it and log `WARN ai.gateway.unknown_term tipo=ENFERMEDAD valor="Frobozz syndrome" specialty=cardiologia`. We do not fail the whole call. Reasons:

- LLMs occasionally hallucinate even with explicit constraints. A 95%-good response should still produce 95% of the tags.
- We do fail when **zero** tags survive — at that point the result is useless to the doctor.

### 4. `especialidadId` validation is strict

Unlike tags, the classification result must be one of the candidate specialties. An unknown id is a clear LLM failure (the candidates were explicit JSON in the prompt) and indicates a malformed response that we should not silently accept. Mapped to 502.

### 5. AI calls inside the DB transaction

`AnalyzeArticleWithAiService` is `@Transactional`. The Groq round-trips happen inside the transaction. This is a known anti-pattern (long-held DB connection) but is acceptable here because:

- Manual trigger only — no high concurrency.
- Groq is fast (typically <3 s per call), making the held connection short-lived.
- The alternative (split flow) introduces races and complexity that don't pay off at MVP scale.
- If concurrency becomes a problem, we revisit by introducing a "stage analysis result, then commit" two-phase flow.

Documented so future maintainers don't "fix" it without understanding the trade-off.

### 6. `HttpClient` over Quarkus REST Client

JDK 21's `HttpClient` gives us per-request timeouts, native async support, and zero Quarkus-specific scaffolding. Quarkus REST Client would require a `@RegisterRestClient` interface and reactive types for a single integration point. The complexity savings are real.

### 7. Observability via response fields, not metrics

`modelUsed`, `promptTokens`, `completionTokens` are surfaced in the API response. We considered Prometheus counters but kept them out of this MVP — usage analysis can be done by tailing the gateway's INFO logs which include the same data. When usage grows, a `Metrics` instrument is a small follow-up.

### 8. Fail-fast on missing API key at boot

A misconfigured `ai.groq.api-key` would otherwise surface as a 502 on the first analyze call — confusing for the operator and visible to the doctor. Failing at boot puts the error in the deployment log where operators look first.

### 9. The matching query loses NULL-on-either-side rows

Pre-existing articles without `especialidad_id` will not match anything until analyzed. Pre-existing contexts without `especialidad_id` will not match anything until re-created (no PATCH endpoint for context — the doctor would re-add it under their newly-assigned specialty). This is a deliberate behavior change documented in feature 1's "Cross-feature impact" and reaffirmed here. Operationally, it means the matching feature appears empty for a brief window after deploy until clinical content is re-tagged.

### 10. `withAiAnalysis` lives on the domain, not on the service

The atomic-replacement intent ("set specialty + replace tags") is a domain operation, not an orchestration concern. Putting it on `Article` keeps the service one line:

```java
article = article.withAiAnalysis(result.especialidadId(), domainTags);
articleRepo.save(article);
```

If a future test needs to construct an analyzed article from scratch, the same method works.

### 11. Groq's OpenAI-compatible API, not a multi-provider abstraction

Groq exposes an OpenAI-compatible endpoint, so the request/response shapes are OpenAI-style (Bearer auth, `messages` array with `system` + `user` roles, `usage.prompt_tokens` / `usage.completion_tokens`). We deliberately do **not** add a provider-agnostic abstraction (e.g. a "ChatCompletion" interface with Anthropic / OpenAI / Groq backends) — YAGNI. The domain port `AiAnalysisGateway` is provider-agnostic at the *behavior* level (it talks about "analyze article", not "POST chat completion"); the impl is hard-wired to Groq. If a second provider becomes a real requirement, a new spec introduces the abstraction at that point.

### 12. JSON mode over prompt-only structure

Groq honors `response_format: { "type": "json_object" }` by constraining the model's decoder to emit only valid JSON. We rely on it for both classification and extraction calls. This removes a class of "the LLM wrapped the JSON in prose" parsing failures that the Anthropic-targeted earlier draft of this spec had to defend against with a brace-balanced extractor.

## Open technical decisions / risks

- **Free-tier 429 rate limiting.** No retries with backoff in MVP — a 429 surfaces as a 502 to the doctor with a clear message ("AI service rate limit exceeded, try again in a minute"). If demos hit this regularly, we add exponential backoff inside the gateway and surface a 503 with `Retry-After` instead.
- **Prompt injection in article abstracts.** Hostile abstracts could try to manipulate the LLM (`"Ignore previous instructions and …"`). Mitigations: we wrap the abstract in clear delimiters in the prompt and instruct the LLM to treat the inner text as data. We do not have a perfect defense; the worst case is a wrong tag, which the doctor catches and removes.
- **Specialty drift.** If new specialties are added at runtime (via feature 1's COO endpoint) without vocabulary JSON files, they appear in the classification candidates but have an empty vocabulary. Step 2 would then produce zero tags → `AiAnalysisException` 502. The operator must ship a vocabulary file in the next release. Flagged in the requirements but not blocking.
- **LLM picks an extinct vocabulary for an article that better fits a stub specialty.** Because the prompt presents all specialty descriptors equally, a borderline article might be classified into a stub specialty with 4 terms, then step 2 finds nothing. Mitigation: the gateway includes `descripcion` in the descriptor; richer descriptions improve classification. Long-term mitigation: fill in vocabularies.
- **Provider lock-in.** Hard-wiring the impl to Groq means a migration to another provider is a rewrite of `GroqAiAnalysisGateway`. Acceptable for MVP — the rewrite scope is one class plus the config block; the domain layer is unaffected.
