# Feature: Article AI Analysis — Implementation Tasks

Execute in order. Each item ≈ one commit. Use TDD per [conventions/testing.md](../../conventions/testing.md).

Depends on features 1 (`specialty`) and 2 (`medical-vocabulary`) being merged. Feature 4 (`consulta-ai-analysis`) also uses the same `AiAnalysisGateway` shipped here — coordinate so both features land in the same release window.

The operator runs all test commands.

## Shared AI types (`domain/shared/`)

- [ ] Add `domain/shared/model/SpecialtyDescriptor.java` (record `(UUID id, String nombre, String slug, String descripcion)`)
- [ ] Add `domain/shared/model/ExtractedTag.java` (record `(TipoClinico tipo, String valor)`)
- [ ] Add `domain/shared/model/ArticleAnalysisRequest.java` (record per `design.md`, with `hasAbstract()` helper)
- [ ] Add `domain/shared/model/ArticleAnalysisResult.java` (record `(UUID especialidadId, List<ExtractedTag> tags, String modelUsed, int promptTokens, int completionTokens, boolean hadAbstract)`)
- [ ] Add `domain/shared/model/ConsultaAnalysisRequest.java` (record `(String consultaText, Vocabulary vocabulary)`)
- [ ] Add `domain/shared/model/ConsultaAnalysisResult.java` (record `(List<ExtractedTag> tags, String modelUsed, int promptTokens, int completionTokens)`)
- [ ] Define `domain/shared/repository/AiAnalysisGateway.java` with the two methods listed in `design.md`

## Domain (`domain/articleaianalysis/`)

- [ ] Add `domain/articleaianalysis/exception/AiAnalysisException.java`
- [ ] Add `domain/articleaianalysis/exception/AiAnalysisTimeoutException.java`
- [ ] Add `domain/articleaianalysis/exception/AiConfigurationException.java`
- [ ] Define `domain/articleaianalysis/usecase/AnalyzeArticleWithAiUseCase.java` (`execute(UUID articleId)` → `Article`)

## Domain (`domain/article/`) — additions on the existing model

- [ ] Update `ArticleTest` to cover the new `withAiAnalysis(...)` behavior: returns a new instance with the supplied `especialidadId` and `tags` (full replacement); does not mutate the original; preserves all other fields → **RED**
- [ ] Implement `Article.withAiAnalysis(UUID especialidadId, List<ArticleTag> tags)` → **GREEN**

## Application (`application/articleaianalysis/`)

- [ ] Write `AnalyzeArticleWithAiServiceTest` (Mockito) covering:
  - Happy path: article has abstract, gateway returns valid `(especialidadId, tags, hadAbstract=true)`, repository called with replaced tags + new especialidadId.
  - Article missing → `ArticleNotFoundException`.
  - **Abstract-less degraded path**: `abstractText` is blank but `titulo` is non-blank → service calls the gateway with the same request, gateway result has `hadAbstract=false`, repository called normally. Assert no exception.
  - Title + keywords only (no abstract): same as above, `hadAbstract=false`.
  - Article exists but `titulo`, `abstractText`, **and** `keywords` are all blank/null → `InvalidArticleDataException`. Gateway must not be called (`verifyNoInteractions(aiGateway)`).
  - Gateway throws `AiAnalysisException` → propagated.
  - Gateway throws `AiAnalysisTimeoutException` → propagated.
  - The vocabulary lookup is driven by the chosen specialty id (assert the mock interactions).
  → **RED**
- [ ] Implement `application/articleaianalysis/AnalyzeArticleWithAiService.java` (`@ApplicationScoped`, `@Transactional`; injects `ArticleRepository`, `SpecialtyRepository`, `VocabularyRepository`, `AiAnalysisGateway`) → **GREEN**

## Infrastructure (`infrastructure/ai/`)

- [ ] Add `application.properties` keys: `ai.groq.api-key`, `ai.groq.model`, `ai.groq.max-tokens`, `ai.groq.timeout`, `ai.groq.base-url` (with defaults per `design.md`)
- [ ] Add `%test.ai.groq.api-key=test-stub` to `application.properties` so `@QuarkusTest` boots without a real key (the gateway is mocked in tests)
- [ ] Write `GroqAiAnalysisGatewayConfigTest` (unit, pure JUnit) covering: missing api-key → `AiConfigurationException`; blank api-key → `AiConfigurationException`; valid config → no throw → **RED**
- [ ] Implement `GroqAiAnalysisGatewayConfig` (`@ApplicationScoped`, observes `StartupEvent`; validates config; exposes config record for the gateway to consume) → **GREEN**
- [ ] Write `GroqAiAnalysisGatewayTest` (unit, pure JUnit, uses WireMock or a hand-rolled `HttpClient`-fake) covering:
  - **Article happy path (full input)**: request has non-blank `titulo` + `abstractText` + `keywords`; classify call returns `{ "especialidadId": "<known-uuid>" }` (Groq JSON mode), extract call returns 3 valid tags → returns `ArticleAnalysisResult` with 3 tags and `hadAbstract=true`. Prompt body sent to Groq contains all three labelled sections (`[TÍTULO]`, `[ABSTRACT]`, `[KEYWORDS]`) — assert via WireMock request-body matcher.
  - **Article degraded path (no abstract)**: request has non-blank `titulo` (and optional `keywords`) but blank `abstractText`; classify and extract still succeed → result has `hadAbstract=false`. Prompt body contains `[TÍTULO]` (and `[KEYWORDS]` if present) but no `[ABSTRACT]` section.
  - **Article degraded path (title only)**: request has only `titulo` non-blank; same behavior — `hadAbstract=false`, prompt body contains only `[TÍTULO]`.
  - Unknown `especialidadId` (not in candidates) → `AiAnalysisException`.
  - Classify response is valid JSON but missing the `especialidadId` field → `AiAnalysisException`.
  - Extract response is valid JSON but missing the `tags` field → `AiAnalysisException`.
  - Extract returns 3 tags, 2 are not in the vocabulary → result has only the 1 valid tag, WARN logged for each filtered tag.
  - Extract returns only tags that fail the vocabulary check → `AiAnalysisException` ("no valid tags after vocabulary filter").
  - Groq returns HTTP 429 (free-tier rate limit) → `AiAnalysisException` carrying the status in the message.
  - Groq returns HTTP 500 → `AiAnalysisException`.
  - Groq returns HTTP 401 (invalid api-key) → `AiAnalysisException` with a hint that the key is wrong.
  - Request exceeds `ai.groq.timeout` → `AiAnalysisTimeoutException`.
  - `usage.prompt_tokens` and `usage.completion_tokens` from the Groq response are captured in the result.
  - Request body sent to Groq includes `response_format: { type: "json_object" }`, the configured `model`, and the configured `max_tokens` (assert via WireMock request matchers).
  - Request includes `Authorization: Bearer <api-key>` header.
  - **Consulta happy path**: extract returns 2 valid tags → returns `ConsultaAnalysisResult` with 2 tags.
  - Consulta extract: missing-fields / vocabulary-filtered-to-empty cases mirror the article extract behavior.
  → **RED**
- [ ] Implement `infrastructure/ai/GroqAiAnalysisGateway` (`@ApplicationScoped` implements `AiAnalysisGateway`):
  - Builds a single `HttpClient` with `connectTimeout` from config.
  - Owns two prompt template files under `src/main/resources/prompts/`: `classify-article.txt`, `extract-tags.txt`.
  - JSON envelope (de)serialization via Jackson.
  - Sends Bearer auth header and `response_format: { type: "json_object" }` on every call.
  - Builds the article-prompt input by concatenating whichever of `titulo`, `abstractText`, `keywords` are non-blank under labelled headers (`[TÍTULO]`, `[ABSTRACT]`, `[KEYWORDS]`); skips blank sections entirely so the prompt does not contain empty headers.
  - `analyzeArticle(request)` executes step 1 (classify) then step 2 (extract using the chosen vocabulary). Sets `hadAbstract = request.hasAbstract()` on the returned result.
  - `analyzeConsultaText(request)` executes one extract call.
  - Logs at INFO on success (article id or consulta excerpt hash, model, `prompt_tokens`, `completion_tokens`, `hadAbstract` for article calls); at WARN on filtered terms; at ERROR on transport failures before wrapping into the domain exception.
  → **GREEN**
- [ ] Add `infrastructure/ai/GroqEnvelope` records (small Jackson-mapped types for the OpenAI-compatible request/response shape — not exposed outside this package). Includes `GroqChatRequest`, `GroqMessage`, `GroqResponseFormat`, `GroqChatResponse`, `GroqChoice`, `GroqUsage`.
- [ ] Add `src/main/resources/prompts/classify-article.txt` and `extract-tags.txt` with the prompt templates documented in `design.md`. Use `{{placeholder}}` syntax for substitution. Both prompts must explicitly instruct the model to "respond with a single JSON object matching the schema below" — required by Groq's JSON mode.

## Article repository update (cross-feature)

- [ ] Update `ArticleRepositoryImplTest` to cover the new SQL in `findMatchingByPatientContext(...)`:
  - Article with `especialidad_id = X` + context with `especialidad_id = X` (same tag) → match.
  - Article with `especialidad_id = X` + context with `especialidad_id = Y` (same tag) → no match.
  - Article with `especialidad_id = NULL` + any context → no match.
  - Context with `especialidad_id = NULL` + any article → no match.
  - Multiple contexts for the same patient, different specialties — only same-specialty articles match each one.
  → **RED**
- [ ] Update `infrastructure/persistence/article/ArticleRepositoryImpl.findMatchingByPatientContext(...)` SQL to add `AND c.especialidad_id = a.especialidad_id` to the join → **GREEN**
- [ ] Re-run the existing `ArticleResourceIT` matching test to confirm it still passes with at least one (article, context) pair that shares a specialty; update test fixtures if needed to add the new `especialidad_id` field to existing setup helpers.

## REST (`interfaces/rest/articleaianalysis/`)

- [ ] Write `AnalyzeArticleResourceIT` (`@QuarkusTest` + RestAssured) covering one test per status code:
  - 200 happy path with abstract (mock the gateway via `QuarkusMock.installMockForType(...)` to return a fixed `ArticleAnalysisResult` with `hadAbstract=true`) — response body includes `hadAbstract: true`.
  - 200 abstract-less path: article fixture has blank `abstractText` but non-blank `titulo`; mocked gateway returns `hadAbstract=false`; response body includes `hadAbstract: false` and a non-empty `tags` array.
  - 400 when the article fixture has `titulo`, `abstractText`, and `keywords` all blank/null.
  - 401 unauthenticated.
  - 404 on unknown article id.
  - 502 when the mocked gateway throws `AiAnalysisException`.
  - 504 when the mocked gateway throws `AiAnalysisTimeoutException`.
  - Response body includes `articleId`, `especialidadId`, `especialidadNombre`, `tags`, `modelUsed`, `promptTokens`, `completionTokens`, `hadAbstract`.
  - Tags are persisted: a follow-up `GET /api/articles/{id}` shows the new tag list and `especialidadId`.
  → **RED**
- [ ] Implement `interfaces/rest/articleaianalysis/AnalyzedTagResponse.java` (record per `design.md`)
- [ ] Implement `interfaces/rest/articleaianalysis/AnalyzeArticleResponse.java` (record)
- [ ] Implement `interfaces/rest/articleaianalysis/AnalyzeArticleRestMapper.java` (`toResponse(Article, ArticleAnalysisResult)`)
- [ ] Implement `interfaces/rest/articleaianalysis/AnalyzeArticleResource.java`:
  - `@Path("/api/articles/{id}/analyze")`, `POST`, no body.
  - Delegates to `AnalyzeArticleWithAiUseCase`.
  - `@Tag("Articles")`, full `@APIResponse` annotations.
  → **GREEN**
- [ ] Wire the three new exceptions into `infrastructure/config/GlobalExceptionHandler` (`AiAnalysisException → 502 AI_ANALYSIS_FAILED`, `AiAnalysisTimeoutException → 504 AI_ANALYSIS_TIMEOUT`). `AiConfigurationException` is **not** wired — it only fires at startup.
- [ ] Verify Swagger UI lists the new endpoint under `Articles`.

## Verification

- [ ] `./mvnw compile` clean
- [ ] `./mvnw test` all green
- [ ] `./mvnw verify -DskipITs=false` all green
- [ ] Coverage ≥ 80% on `domain/articleaianalysis` and `application/articleaianalysis`
- [ ] Manual smoke test (operator, with `GROQ_API_KEY` set in env — obtain a free key at <https://console.groq.com/keys>):
  - Trigger `POST /api/articles/{id}/analyze` for one of the PubMed-imported cardiología articles via Swagger. Confirm `200 OK` with non-empty tags, `especialidadId = cardiología`, and `hadAbstract: true`.
  - Pick a PubMed-imported article whose `abstractText` is empty (letter / editorial — easy to find via SQL `SELECT id FROM articulos_cientificos WHERE abstract_text IS NULL OR TRIM(abstract_text) = '' LIMIT 1`). Trigger `POST /api/articles/{id}/analyze` on it. Confirm `200 OK` with `hadAbstract: false` and at least one tag.
  - Confirm `GET /api/articles/{id}` reflects the saved state for both cases.
  - Confirm `GET /api/patients/{p}/matching-articles` now returns these articles for a cardiology-tagged patient.
- [ ] Negative smoke test: unset `GROQ_API_KEY` and confirm `./mvnw quarkus:dev` aborts at boot with `AiConfigurationException`.

## Wrap-up

- [ ] **Do not** write `summary.md` — filled post-implementation in a future session.
