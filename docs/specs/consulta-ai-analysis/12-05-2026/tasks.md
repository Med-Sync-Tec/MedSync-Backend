# Feature: Consulta AI Analysis — Implementation Tasks

Execute in order. Each item ≈ one commit. Use TDD per [conventions/testing.md](../../../conventions/testing.md).

Depends on features 1 (`specialty`), 2 (`medical-vocabulary`), and 3 (`article-ai-analysis`) being merged. The shared `AiAnalysisGateway` and `ClaudeAiAnalysisGateway` from feature 3 are reused as-is. This feature adds:

- A new analyze endpoint under `/api/consultas/{consultaId}/analyze`.
- A new bulk endpoint under `/api/patients/{patientId}/contextos/bulk` (cross-feature addition to `patient-context`).
- Two new domain exceptions.

The operator runs all test commands.

## Domain (`domain/consultaaianalysis/`)

- [ ] Write `ConsultaAnalysisTest` (unit, pure JUnit) covering: constructor accepts a `POPULATED` status with non-empty suggestions; accepts an `EMPTY` status with empty suggestions and zero token counts; rejects an `EMPTY` status with non-empty suggestions (invariant — empty status means no LLM call was made) → **RED**
- [ ] Implement `domain/consultaaianalysis/model/ConsultaAnalysis.java` + `domain/consultaaianalysis/model/VocabularyStatus.java` → **GREEN**
- [ ] Add `domain/consultaaianalysis/exception/UserHasNoSpecialtyException.java`
- [ ] Add `domain/consultaaianalysis/exception/InvalidConsultaDataException.java`
- [ ] Define `domain/consultaaianalysis/usecase/AnalyzeConsultaWithAiUseCase.java` (`execute(String consultaId, User caller) → ConsultaAnalysis`)

## Application (`application/consultaaianalysis/`)

- [ ] Write `ConsultaSoapJoinerTest` (unit, pure JUnit; either as a static helper method on the service or a separate utility — agent chooses). Cover:
  - All seven fields populated → joined string contains all seven headers and values in the documented order.
  - Each field individually null/blank → that section uses `"(no registrado)"`.
  - All seven fields null/blank → returns an empty signal so the caller can throw `InvalidConsultaDataException`.
  - Truncation at 50,000 chars: input of 60,000 chars → output exactly 50,000; signal carries `wasTruncated = true` and `originalLength = 60000`.
  → **RED**
- [ ] Implement the SOAP joiner (recommend: package-private static helper in `application/consultaaianalysis/`) → **GREEN**
- [ ] Write `AnalyzeConsultaWithAiServiceTest` (Mockito) covering:
  - Happy path with populated vocabulary: gateway is called once, returns 3 tags; result has `POPULATED` status and 3 suggestions.
  - Caller has `especialidadId = null` → `UserHasNoSpecialtyException`, gateway not called.
  - Consulta not found in hospital → `ConsultaNotFoundException`, gateway not called.
  - Consulta exists but all SOAP fields blank → `InvalidConsultaDataException`, gateway not called.
  - Caller's vocabulary has `totalTerms() == 0` → result is `EMPTY` with empty suggestions, **gateway not called** (assert `verifyNoInteractions(aiGateway)`).
  - Gateway throws `AiAnalysisException` → propagated.
  - Gateway throws `AiAnalysisTimeoutException` → propagated.
  - Truncated SOAP text is forwarded to the gateway unchanged from the joiner.
  → **RED**
- [ ] Implement `application/consultaaianalysis/AnalyzeConsultaWithAiService.java` (`@ApplicationScoped`, **not** `@Transactional` — read-only path. Injects `HospitalGateway`, `VocabularyRepository`, `AiAnalysisGateway`) → **GREEN**

## REST (`interfaces/rest/consultaaianalysis/`)

- [ ] Write `AnalyzeConsultaResourceIT` (`@QuarkusTest` + RestAssured; uses `QuarkusMock` to install a stub `AiAnalysisGateway`) covering:
  - 200 happy path with populated vocabulary — response contains `consultaId`, `especialidadId`, `especialidadSlug`, `vocabularyStatus: "POPULATED"`, `suggestions` array, `modelUsed`, `inputTokens`, `outputTokens`.
  - 200 with `vocabularyStatus: "EMPTY"` and `suggestions: []` when the caller's specialty has an empty vocabulary (drive by a test-only seeded specialty with no JSON file).
  - 400 `USER_HAS_NO_SPECIALTY` when the caller's user has `especialidadId = null`.
  - 400 `INVALID_CONSULTA_DATA` when the hospital returns a consulta with all-blank SOAP fields.
  - 404 when the consulta id is unknown.
  - 401 unauthenticated.
  - 502 when the mocked gateway throws `AiAnalysisException`.
  - 504 when the mocked gateway throws `AiAnalysisTimeoutException`.
  - No data was persisted: assert that follow-up `GET /api/patients/{p}/contextos` is unchanged from baseline.
  → **RED**
- [ ] Implement `interfaces/rest/consultaaianalysis/SuggestionResponse.java` (record)
- [ ] Implement `interfaces/rest/consultaaianalysis/ConsultaAnalysisResponse.java` (record)
- [ ] Implement `interfaces/rest/consultaaianalysis/ConsultaAnalysisRestMapper.java` (`toResponse(ConsultaAnalysis)`)
- [ ] Implement `interfaces/rest/consultaaianalysis/AnalyzeConsultaResource.java`:
  - `@Path("/api/consultas/{consultaId}/analyze")`, `POST`, no body.
  - Reads `AuthenticatedUserContext` for the caller.
  - Delegates to `AnalyzeConsultaWithAiUseCase`.
  - `@Tag("Hospital")`, full `@APIResponse` annotations.
  → **GREEN**
- [ ] Wire `UserHasNoSpecialtyException → 400 USER_HAS_NO_SPECIALTY` and `InvalidConsultaDataException → 400 INVALID_CONSULTA_DATA` into `infrastructure/config/GlobalExceptionHandler`
- [ ] Verify Swagger UI lists the analyze endpoint under `Hospital`.

## Cross-feature impact — `patient-context` bulk endpoint

- [ ] Define `domain/pacientecontexto/usecase/BulkAddPacienteContextoUseCase.java` (`execute(UUID patientId, List<BulkEntry> entries, User caller) → List<PacienteContexto>`)
- [ ] Add `domain/pacientecontexto/model/BulkEntry.java` (record `(String tipo, String valor)`) — kept in domain so the use case interface stays framework-free
- [ ] Write `BulkAddPacienteContextoServiceTest` (Mockito) covering:
  - Happy path with 3 entries, all valid → repository called 3 times, all rows have `especialidadId = caller.user.especialidadId`.
  - Caller has `especialidadId = null` → all rows persisted with `especialidadId = null` (no exception — null is allowed for callers without a specialty).
  - Patient missing → `PatientNotFoundException`, repository never called.
  - Entry #2 of 5 has an unknown `tipo` → `InvalidPacienteContextoDataException`, **zero rows persisted** (transactional rollback via Mockito `inOrder` verification that save was called for #1 then the exception fires before #2 is saved).
  - Empty `entries` list → `InvalidPacienteContextoDataException` (defense-in-depth — the DTO `@NotEmpty` covers this at the boundary, but the service also guards).
  → **RED**
- [ ] Implement `application/pacientecontexto/BulkAddPacienteContextoService.java` (`@ApplicationScoped`, `@Transactional`; injects `PatientRepository`, `PacienteContextoRepository`) → **GREEN**
- [ ] Implement `interfaces/rest/pacientecontexto/BulkAddPacienteContextoRequest.java` (`@NotEmpty @Size(max=50) @Valid public List<EntryDto> entries;` with nested `EntryDto`)
- [ ] Update `PacienteContextoRestMapper` if needed (most likely the existing `toResponse(PacienteContexto)` is reused unchanged)
- [ ] Update `PacienteContextoResource` to add `POST /api/patients/{patientId}/contextos/bulk`:
  - Maps to `BulkAddPacienteContextoUseCase`.
  - Reads caller from `AuthenticatedUserContext`.
  - Returns `201 Created` with `List<PacienteContextoResponse>`.
  - `@Tag("Patient Context")`, full `@APIResponse` annotations.
- [ ] Extend the existing `PacienteContextoResourceIT` (or add `BulkPacienteContextoResourceIT`) covering:
  - 201 happy path with 3 entries → response array has 3 items, each with the caller's `especialidadId`.
  - 400 when `entries` is empty.
  - 400 when `entries` has 51 items (cap violated).
  - 400 when an entry has unknown `tipo` → response status, no rows persisted (assert follow-up `GET` shows baseline).
  - 400 when an entry has `valor > 500 chars`.
  - 404 when `patientId` is unknown.
  - 401 unauthenticated.

## Verification

- [ ] `./mvnw compile` clean
- [ ] `./mvnw test` all green
- [ ] `./mvnw verify -DskipITs=false` all green
- [ ] Coverage ≥ 80% on `domain/consultaaianalysis`, `application/consultaaianalysis`, and the added `BulkAddPacienteContextoService` in `application/pacientecontexto`
- [ ] Manual smoke test (operator, with `ANTHROPIC_API_KEY` set):
  - Pick a consulta id from the hospital dev DB.
  - As a cardiology doctor (whose `User.especialidadId = cardiología`), call `POST /api/consultas/{id}/analyze`.
  - Confirm 200 with non-empty suggestions (assuming `cardiologia.json` is populated).
  - As the same doctor, call `POST /api/patients/{p}/contextos/bulk` with the suggestions.
  - Confirm 201 with persisted rows, each carrying `especialidadId = cardiología`.
  - Call `GET /api/patients/{p}/matching-articles` — confirm the patient now matches cardiology-analyzed articles.
- [ ] Negative smoke test: create a temporary user with `especialidadId = null` (or remove your user's specialty via direct SQL), authenticate as that user, hit the analyze endpoint, confirm 400 `USER_HAS_NO_SPECIALTY`.

## Wrap-up

- [ ] **Do not** write `summary.md` — filled post-implementation in a future session.
