# Feature: Article AI Analysis — Implementation Tasks

Execute in order. Each item ≈ one commit. Use TDD per [conventions/testing.md](../../../conventions/testing.md).

Depends on features 1 (`specialty`) and 2 (`medical-vocabulary`) being merged. Feature 4 (`consulta-ai-analysis`) also uses the same `AiAnalysisGateway` shipped here — coordinate so both features land in the same release window.

The operator runs all test commands.

## Shared AI types (`domain/shared/`)

- [ ] Add `domain/shared/model/SpecialtyDescriptor.java` (record `(UUID id, String nombre, String slug, String descripcion)`)
- [ ] Add `domain/shared/model/ExtractedTag.java` (record `(TipoClinico tipo, String valor)`)
- [ ] Add `domain/shared/model/ArticleAnalysisRequest.java` (record per `design.md`)
- [ ] Add `domain/shared/model/ArticleAnalysisResult.java` (record `(UUID especialidadId, List<ExtractedTag> tags, String modelUsed, int inputTokens, int outputTokens)`)
- [ ] Add `domain/shared/model/ConsultaAnalysisRequest.java` (record `(String consultaText, Vocabulary vocabulary)`)
- [ ] Add `domain/shared/model/ConsultaAnalysisResult.java` (record `(List<ExtractedTag> tags, String modelUsed, int inputTokens, int outputTokens)`)
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
  - Happy path: article exists, abstract non-blank, gateway returns valid `(especialidadId, tags)`, repository called with replaced tags + new especialidadId.
  - Article missing → `ArticleNotFoundException`.
  - Article exists but `abstractText` is blank / null / whitespace-only → `InvalidArticleDataException`.
  - Gateway throws `AiAnalysisException` → propagated.
  - Gateway throws `AiAnalysisTimeoutException` → propagated.
  - The vocabulary lookup is driven by the chosen specialty id (assert the mock interactions).
  → **RED**
- [ ] Implement `application/articleaianalysis/AnalyzeArticleWithAiService.java` (`@ApplicationScoped`, `@Transactional`; injects `ArticleRepository`, `SpecialtyRepository`, `VocabularyRepository`, `AiAnalysisGateway`) → **GREEN**

## Infrastructure (`infrastructure/ai/`)

- [ ] Add `application.properties` keys: `ai.anthropic.api-key`, `ai.anthropic.model`, `ai.anthropic.max-tokens`, `ai.anthropic.timeout`, `ai.anthropic.base-url` (with defaults per `design.md`)
- [ ] Add `%test.ai.anthropic.api-key=test-stub` to `application.properties` so `@QuarkusTest` boots without a real key (the gateway is mocked in tests)
- [ ] Write `ClaudeAiAnalysisGatewayConfigTest` (unit, pure JUnit) covering: missing api-key → `AiConfigurationException`; blank api-key → `AiConfigurationException`; valid config → no throw → **RED**
- [ ] Implement `ClaudeAiAnalysisGatewayConfig` (`@ApplicationScoped`, observes `StartupEvent`; validates config; exposes config record for the gateway to consume) → **GREEN**
- [ ] Write `ClaudeAiAnalysisGatewayTest` (unit, pure JUnit, uses WireMock or a hand-rolled `HttpClient`-fake) covering:
  - **Article happy path**: classify call returns `{ "especialidadId": "<known-uuid>" }`, extract call returns 3 valid tags → returns `ArticleAnalysisResult` with 3 tags.
  - Unknown `especialidadId` (not in candidates) → `AiAnalysisException`.
  - Malformed JSON in classify response (e.g. text with no JSON object) → `AiAnalysisException`.
  - Malformed JSON in extract response → `AiAnalysisException`.
  - Extract returns 3 tags, 2 are not in the vocabulary → result has only the 1 valid tag, WARN logged for each filtered tag.
  - Extract returns only tags that fail the vocabulary check → `AiAnalysisException` ("no valid tags after vocabulary filter").
  - Anthropic returns HTTP 429 → `AiAnalysisException` carrying the status in the message.
  - Anthropic returns HTTP 500 → `AiAnalysisException`.
  - Request exceeds `ai.anthropic.timeout` → `AiAnalysisTimeoutException`.
  - `usage.input_tokens` and `usage.output_tokens` from the Anthropic response are captured in the result.
  - **Consulta happy path**: extract returns 2 valid tags → returns `ConsultaAnalysisResult` with 2 tags.
  - Consulta extract: malformed JSON / vocabulary-filtered-to-empty cases mirror the article extract behavior.
  → **RED**
- [ ] Implement `infrastructure/ai/ClaudeAiAnalysisGateway` (`@ApplicationScoped` implements `AiAnalysisGateway`):
  - Builds a single `HttpClient` with `connectTimeout` from config.
  - Owns two prompt template files under `src/main/resources/prompts/`: `classify-article.txt`, `extract-tags.txt`.
  - JSON envelope (de)serialization via Jackson.
  - `analyzeArticle(request)` executes step 1 (classify) then step 2 (extract using the chosen vocabulary).
  - `analyzeConsultaText(request)` executes one extract call.
  - Logs at INFO on success (article id or consulta excerpt hash, model, token counts); at WARN on filtered terms; at ERROR on transport failures before wrapping into the domain exception.
  → **GREEN**
- [ ] Add `infrastructure/ai/AnthropicEnvelope` records (small Jackson-mapped types for the request/response shape — not exposed outside this package)
- [ ] Add `src/main/resources/prompts/classify-article.txt` and `extract-tags.txt` with the prompt templates documented in `design.md`. Use `{{placeholder}}` syntax for substitution.

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
  - 200 happy path (mock the gateway via `QuarkusMock.installMockForType(...)` to return a fixed `ArticleAnalysisResult`).
  - 400 on article with no abstract.
  - 401 unauthenticated.
  - 404 on unknown article id.
  - 502 when the mocked gateway throws `AiAnalysisException`.
  - 504 when the mocked gateway throws `AiAnalysisTimeoutException`.
  - Response body includes `articleId`, `especialidadId`, `especialidadNombre`, `tags`, `modelUsed`, `inputTokens`, `outputTokens`.
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
- [ ] Manual smoke test (operator, with `ANTHROPIC_API_KEY` set in env):
  - Trigger `POST /api/articles/{id}/analyze` for one of the PubMed-imported cardiología articles via Swagger.
  - Confirm `200 OK` with non-empty tags and `especialidadId = cardiología`.
  - Confirm `GET /api/articles/{id}` reflects the saved state.
  - Confirm `GET /api/patients/{p}/matching-articles` now returns this article for a cardiology-tagged patient.
- [ ] Negative smoke test: unset `ANTHROPIC_API_KEY` and confirm `./mvnw quarkus:dev` aborts at boot with `AiConfigurationException`.

## Wrap-up

- [ ] **Do not** write `summary.md` — filled post-implementation in a future session.
