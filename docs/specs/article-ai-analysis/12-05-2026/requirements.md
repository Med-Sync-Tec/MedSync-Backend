# Feature: Article AI Analysis — Requirements

## Overview

A doctor-triggered endpoint that uses Claude Haiku 4.5 to (a) classify a scientific article into one of MedSync's medical specialties and (b) extract a constrained set of clinical tags from the article's abstract, drawing from that specialty's controlled vocabulary. The endpoint **auto-saves** both the chosen specialty and the extracted tags onto the article, replacing any prior AI- or manual-tag state for that article. The doctor refines mistakes with the existing tag-remove endpoint.

This feature also tightens the article-to-patient matching query so it joins on `articulo.especialidad_id = paciente_contexto.especialidad_id`, scoping every patient's match list to articles in their context's specialty.

## User stories

- As a **doctor**, I want to click "Analyze with AI" on an article, so that the article is automatically classified by specialty and tagged with vocabulary-bounded clinical terms.
- As a **doctor**, I want to correct AI mistakes by removing wrong tags via the existing endpoint, so that I do not need a separate "re-tag" workflow.
- As a **system**, I want article matching to a patient to be scoped by specialty, so that a patient with only cardiology context does not see oncology articles.
- As an **operator**, I want the AI integration to be configurable via `application.properties` (api key, model, max tokens, timeout), so that we can swap models or rotate keys without a code change.

## Acceptance criteria (EARS format)

### Ubiquitous

- The system shall expose `POST /api/articles/{id}/analyze` to any authenticated user.
- The system shall use Anthropic Claude Haiku 4.5 via raw HTTP (no SDK) to perform the analysis.
- The system shall constrain extracted tags to the set of canonical terms in the chosen specialty's `Vocabulary` (case-insensitive match per `Vocabulary.containsTerm`).
- The system shall **replace** the article's existing tags atomically with the AI-extracted set on each invocation. The `articulo.especialidad_id` is overwritten with the AI's classification.
- The system shall surface analysis cost via observability — the gateway logs the model used, the token counts returned by the Anthropic API, and the analyzed article id at INFO level for every successful call.

### Event-driven

- When a `POST /api/articles/{id}/analyze` request arrives from an authenticated user and the article exists, the system shall:
  1. Load the article (with `abstractText`, `titulo`, `keywords`).
  2. Load all active specialties and their vocabularies.
  3. Call `AiAnalysisGateway.analyzeArticle(...)`, receiving `(especialidadId, List<ExtractedTag>)`.
  4. Validate that `especialidadId` is in the active-specialty set and each `ExtractedTag` is in the chosen specialty's vocabulary.
  5. Persist the article with the new `especialidad_id` and **replaced** tag list in one transaction.
  6. Return `200 OK` with the analysis result.
- When the existing matching query `GET /api/patients/{patientId}/matching-articles` runs, the system shall additionally require `articulo.especialidad_id = paciente_contexto.especialidad_id` in the join.

### Conditional

- If the path `{id}` does not match an existing article, the system shall return `404 Not Found` (`ArticleNotFoundException`).
- If the article has no `abstractText` (null or empty after trim), the system shall return `400 Bad Request` (`InvalidArticleDataException` with message `"Article has no abstract — AI analysis requires non-empty abstractText"`).
- If the Anthropic API returns an unparseable response, an unknown specialty id, or no valid tags after filtering, the system shall return `502 Bad Gateway` (`AiAnalysisException`).
- If the Anthropic API call exceeds the configured `ai.anthropic.timeout`, the system shall return `504 Gateway Timeout` (`AiAnalysisTimeoutException`).
- If the unauthenticated, the system shall return `401 Unauthorized` (already enforced by the auth filter).
- If the configured `ai.anthropic.api-key` is missing or empty at startup, the system shall **abort boot** with a clear log entry — the application cannot serve AI endpoints without it.

### State-driven

- While a matching query runs for a patient whose contexts have `especialidad_id = NULL`, the system shall return zero matches (the new join filter excludes them). The previous behavior — matching purely on `(tipo, valor)` — is intentionally retired.
- While an article has `especialidad_id = NULL` (not yet analyzed), the system shall exclude it from every patient's match list.

## Non-functional requirements

- **Latency**: P95 analysis call ≤ 8 seconds end-to-end (one or two LLM round-trips inside the gateway).
- **Cost**: each analysis consumes one Haiku 4.5 input window (article abstract + specialty descriptors + vocabulary). Estimated < $0.02 per call at current Anthropic pricing.
- **Idempotency**: re-invoking the endpoint on the same article overwrites prior results — this is the documented behavior, not a bug. The frontend warns the user before invoking.
- **Configurability**: `ai.anthropic.api-key`, `ai.anthropic.model`, `ai.anthropic.max-tokens`, `ai.anthropic.timeout` are read from `application.properties`. The default `model` is `claude-haiku-4-5-20251001`. The default `max-tokens` is `2048`. The default `timeout` is `30s`.
- **Test coverage**: ≥ 80% on `domain/articleaianalysis` and `application/articleaianalysis`. The HTTP gateway implementation in `infrastructure/ai/` is covered by stubbed-HTTP unit tests (WireMock or a hand-rolled fake) plus one end-to-end integration test gated behind a profile property (`ai.live=true`) that is OFF by default.

## Out of scope (explicit)

- **Scheduler / batch re-analysis**. AI is manually triggered only. A nightly "auto-tag all new PubMed articles" job is a deferred future spec.
- **Multi-specialty articles**. An article gets exactly one specialty (MVP — N:M deferred).
- **Tag-source tracking** (which tags came from AI vs which were manually added). Replaced semantics make this irrelevant for now.
- **Partial / streaming responses**. The endpoint returns the full analysis in one response.
- **AI usage audit log in the DB**. Analysis events are logged but not persisted as rows.
- **Custom prompt overrides per call**. Prompts are baked into the gateway and version-controlled with the code.
- **Editing the matching query's specialty filter** to allow NULL = NULL or wildcard matching. The strict equality is the intended new behavior.

## Open questions

- Should we support a "dry run" mode (analyze without saving) for debugging? *Not blocking — the consulta-ai-analysis feature already implements review-then-save; if the same pattern is needed for articles, revisit in a follow-up.*
- Should we cache analyses by article+vocabulary version hash to avoid duplicate spend on re-invocations? *Not blocking — cost is low at MVP scale; revisit if monthly spend exceeds budget.*
