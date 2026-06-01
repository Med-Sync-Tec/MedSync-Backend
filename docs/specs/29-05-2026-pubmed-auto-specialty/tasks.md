# Feature: PubMed Auto-Specialty — Implementation Tasks

Execute in order. Each item ≈ one commit. Use TDD per
[conventions/testing.md](../../conventions/testing.md).

Depends on `article-ai-analysis` being fully implemented and merged (the
`AiAnalysisGateway`, `ArticleAnalysisRequest`, `ArticleAnalysisResult`,
`Article.withAiAnalysis`, etc. must already exist).

---

## Domain (`domain/article/`)

- [ ] Add `domain/article/model/AutoAnalysisSummary.java`
  (record `(int attempted, int succeeded, int skippedBlank, int failed)`).
- [ ] Define `domain/article/usecase/AutoAnalyzeNewArticlesUseCase.java`
  (single method `AutoAnalysisSummary execute()`; javadoc per `design.md`).
- [ ] Update `domain/article/repository/ArticleRepository` with the new method:
  `List<Article> findAllWithoutSpecialty()`.

---

## Application (`application/pubmedautospecialty/`)

- [ ] Write `AutoAnalyzeNewArticlesServiceTest` (Mockito) covering:
  - **Happy path**: 2 articles without specialty returned by repo; gateway
    returns valid results for both → `succeeded=2, skippedBlank=0, failed=0`.
  - **Already-classified articles excluded**: repo returns 0 candidates
    (all existing articles already have a specialty) → summary is all zeros,
    gateway is never called (`verifyNoInteractions(aiGateway)`).
  - **Blank article skipped**: one article has all text fields null/blank →
    `skippedBlank=1`, gateway not called for it, other articles still processed.
  - **Gateway exception on one article**: first article succeeds, second throws
    `AiAnalysisException` → `succeeded=1, failed=1`, no exception propagated
    from `execute()`.
  - **Gateway timeout on one article**: throws `AiAnalysisTimeoutException` →
    counted as `failed=1`, no exception propagated.
  - **Feature disabled**: `enabled=false` → `execute()` returns
    `AutoAnalysisSummary(0,0,0,0)`, `findAllWithoutSpecialty` never called.
  - **Specialties/vocabularies loaded once**: with 3 candidate articles, assert
    `findAllActive()` and `getVocabularyFor(...)` are called exactly once per
    specialty (not once per article).
  → **RED**
- [ ] Implement `application/pubmedautospecialty/ArticleSpecialtySaver.java`
  (`@ApplicationScoped`, package-private; `@Transactional(REQUIRES_NEW)` on
  `save(Article, UUID, List<ArticleTag>)`; injects `ArticleRepository`).
- [ ] Implement `application/pubmedautospecialty/AutoAnalyzeNewArticlesService.java`
  (`@ApplicationScoped`, implements `AutoAnalyzeNewArticlesUseCase`; NOT
  `@Transactional` at class level; injects `ArticleRepository`,
  `SpecialtyRepository`, `VocabularyRepository`, `AiAnalysisGateway`,
  `ArticleSpecialtySaver`;
  reads `@ConfigProperty(name="medsync.pubmed.auto-specialty.enabled",
  defaultValue="true") boolean enabled`).
  → **GREEN**

---

## Persistence (`infrastructure/persistence/article/`)

- [ ] Write new test cases in `ArticleRepositoryImplTest`:
  - `findAllWithoutSpecialty` returns only articles with `especialidad_id IS NULL`.
  - `findAllWithoutSpecialty` excludes articles that already have a specialty set.
  - `findAllWithoutSpecialty` results are ordered by `createdAt ASC`.
  - When all articles have a specialty, returns empty list.
  → **RED**
- [ ] Implement `ArticleRepositoryImpl.findAllWithoutSpecialty()` using JPQL
  `"especialidadId IS NULL ORDER BY createdAt ASC"` (Panache `.list()` shorthand).
  → **GREEN**
- [ ] Run `./mvnw compile` and verify Hibernate `validate` boots cleanly (no
  new columns or tables — the query targets an existing nullable column).

---

## Scheduler modification (`infrastructure/pubmed/`)

- [ ] Write `PubmedSyncSchedulerTest` (Mockito + `@QuarkusTest`) covering:
  - After `syncUseCase.execute()` returns, `autoAnalyzeUseCase.execute()` is
    called exactly once.
  - The return value of `autoAnalyzeUseCase.execute()` is logged (assert logger
    or capture via mock).
  - If `syncUseCase.execute()` throws, `autoAnalyzeUseCase.execute()` is **not**
    called (the existing exception handling still applies).
  → **RED**
- [ ] Modify `infrastructure/pubmed/PubmedSyncScheduler.java`:
  - Inject `AutoAnalyzeNewArticlesUseCase autoAnalyzeUseCase` via constructor.
  - In `syncPubmedArticles()`, after the `syncUseCase.execute()` call succeeds,
    call `autoAnalyzeUseCase.execute()` and log the `AutoAnalysisSummary` at
    INFO level.
  - Exception from `autoAnalyzeUseCase.execute()` is caught and logged at ERROR
    (same pattern as the existing sync error handling) — a failed auto-analysis
    pass must not prevent the scheduler from completing.
  → **GREEN**

---

## Configuration

- [ ] Add to `src/main/resources/application.properties`:
  ```properties
  medsync.pubmed.auto-specialty.enabled=true
  ```
- [ ] Add test profile override (in the `%test` block or
  `application.properties`):
  ```properties
  %test.medsync.pubmed.auto-specialty.enabled=false
  ```
  This prevents live Groq calls in `@QuarkusTest` suites that do not explicitly
  mock the gateway.

---

## REST (no new endpoints)

No new REST endpoints. The feature is entirely internal (scheduler → use case →
gateway → repository). The existing `POST /api/articles/{id}/analyze` remains
the manual per-article trigger.

---

## Integration test (`AnalyzeArticleResourceIT` / scheduler IT)

- [ ] Write (or extend) a `PubmedAutoSpecialtyIT` (`@QuarkusTest`) covering the
  end-to-end happy path:
  - Seed 2 articles with `especialidad_id = NULL`.
  - Install a `QuarkusMock` for `AiAnalysisGateway` that returns a fixed
    `ArticleAnalysisResult` (with a known specialty id from the test fixtures).
  - Call `autoAnalyzeUseCase.execute()` directly (inject via `@Inject`).
  - Assert both articles now have `especialidad_id` set in the DB
    (verify via `GET /api/articles/{id}`).
  - Assert the returned `AutoAnalysisSummary` reports `succeeded=2`.
- [ ] Verify the disabled-flag path:
  - Set `medsync.pubmed.auto-specialty.enabled=false` via
    `QuarkusTestProfile` or `@TestHTTPEndpoint` override.
  - Call `autoAnalyzeUseCase.execute()`.
  - Assert gateway mock was never called.

---

## Verification

- [ ] `./mvnw compile` clean.
- [ ] `./mvnw test` all green (includes unit tests from this spec).
- [ ] `./mvnw verify -DskipITs=false` all green (includes integration tests).
- [ ] Coverage ≥ 80% on `application/pubmedautospecialty`.
- [ ] Manual smoke test (operator, with `GROQ_API_KEY` set in env):
  1. Clear `especialidad_id` on a handful of articles via SQL:
     ```sql
     UPDATE articulos_cientificos SET especialidad_id = NULL WHERE id IN (...);
     ```
  2. Trigger `POST /api/articles/sync` (or restart the app) to fire the
     scheduler.
  3. Tail the log and confirm entries:
     - `"Auto-specialty: N articles to analyze"`
     - Per-article: `"Auto-specialty: article <uuid> → specialty <name>"`
     - Summary: `"Auto-specialty pass complete: attempted=N succeeded=N ..."`
  4. Confirm `GET /api/articles/{id}` for the affected articles returns
     `especialidadId` non-null and `tags` non-empty.
  5. In the frontend, confirm article cards now render the specialty color and
     icon without needing to click "Analyze with AI".
- [ ] Negative smoke test:
  - Set `medsync.pubmed.auto-specialty.enabled=false` and re-run the sync.
  - Confirm log contains only `"Auto-specialty disabled by config — skipping."`.
  - Confirm articles remain with `especialidad_id = NULL`.

---

## Wrap-up

- [ ] Update [specs/README.md](../README.md) feature index to add
  `pubmed-auto-specialty | Implemented | 29-05-2026-pubmed-auto-specialty/`.
- [ ] **Do not** write `summary.md` — filled post-implementation.
