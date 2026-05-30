# Feature: PubMed Auto-Specialty — Design

> Spec name: `pubmed-auto-specialty`. Java package: `pubmedautospecialty`
> (single segment, lowercase). This feature adds a post-sync pass that reuses
> the existing `AiAnalysisGateway` and `AnalyzeArticleWithAiService` logic but
> runs outside any sync transaction.

## Summary of the approach

`SyncPubmedArticlesService.execute()` currently returns the count of
inserted/updated articles. After this feature ships, the scheduler
(`PubmedSyncScheduler`) will, immediately after `syncUseCase.execute()` returns,
invoke a new use case `AutoAnalyzeNewArticlesUseCase.execute()`. That use case
queries the repository for all articles with `especialidad_id = NULL`, then
calls the AI gateway for each one sequentially.

The separation of concerns is:
- **Sync transaction** — PubMed fetch + persist (unchanged).
- **Auto-analysis pass** — runs in its own transaction(s), outside the sync
  transaction, so a gateway failure cannot roll back any persisted article.

## Domain additions

### New use case interface (`domain/article/usecase/`)

```java
// domain/article/usecase/AutoAnalyzeNewArticlesUseCase.java
public interface AutoAnalyzeNewArticlesUseCase {
    /**
     * Analyzes every article that has no specialty assigned yet.
     * Each article is analyzed in its own transaction; failures are logged and
     * skipped without aborting the batch.
     *
     * @return AutoAnalysisSummary with counts of attempted / succeeded /
     *         skipped-blank / failed articles.
     */
    AutoAnalysisSummary execute();
}
```

### New value object (`domain/article/model/`)

```java
// domain/article/model/AutoAnalysisSummary.java
public record AutoAnalysisSummary(
    int attempted,      // articles with especialidad_id == null at query time
    int succeeded,      // AI returned valid result, article saved
    int skippedBlank,   // all three text fields were blank — gateway not called
    int failed          // gateway threw (any exception)
) {}
```

No database migration is needed — this record is transient (returned and logged,
never persisted).

### New repository method (`domain/article/repository/ArticleRepository`)

The existing `ArticleRepository` gains one new method:

```java
/**
 * Returns all articles that have no specialty assigned yet (especialidad_id IS NULL).
 * Only live (non-deleted) articles are returned.
 * Order: by createdAt ascending (oldest first — allows a restart to continue
 * from where it left off if the process is interrupted).
 */
List<Article> findAllWithoutSpecialty();
```

No HTTP / AI dependency — purely a data-access concern. Lives in the existing
`ArticleRepository` port.

## Output ports used (no new ports)

| Port                    | Method                                             | Where defined              |
|-------------------------|----------------------------------------------------|----------------------------|
| `ArticleRepository`     | `findAllWithoutSpecialty()` *(new)*                | `domain/article/repository`|
| `ArticleRepository`     | `save(Article)` *(existing)*                       | `domain/article/repository`|
| `SpecialtyRepository`   | `findAllActive()` *(existing)*                     | `domain/specialty/repository`|
| `VocabularyRepository`  | `getVocabularyFor(UUID)` *(existing)*              | `domain/vocabulary/repository`|
| `AiAnalysisGateway`     | `analyzeArticle(ArticleAnalysisRequest)` *(existing)* | `domain/shared/repository`|

## Use cases

| Use case interface             | Service implementation                 | Trigger                                        |
|--------------------------------|----------------------------------------|------------------------------------------------|
| `AutoAnalyzeNewArticlesUseCase`| `AutoAnalyzeNewArticlesService`        | Called by `PubmedSyncScheduler` after each sync |

The existing `AnalyzeArticleWithAiService` implements the per-article logic
already. `AutoAnalyzeNewArticlesService` **does not call
`AnalyzeArticleWithAiService`** directly — it would couple two application
services. Instead, it duplicates the per-article logic inline (load specialties
+ vocabs once; loop over unclassified articles; call gateway; call
`withAiAnalysis`; save). The shared AI interaction types
(`ArticleAnalysisRequest`, `ArticleAnalysisResult`, etc.) are reused from
`domain/shared/model/`.

> **Why not reuse `AnalyzeArticleWithAiService` directly?**
> `AnalyzeArticleWithAiService` fetches specialties and vocabularies **inside
> each call**. Running it 100 times would make 100 × N_specialties vocabulary
> lookups. The batch service loads specialties and vocabularies once before the
> loop and reuses them, which is the correct optimization for a batch context.
> Coupling two application services would also violate the single-responsibility
> principle.

## Application service (`application/pubmedautospecialty/`)

```
AutoAnalyzeNewArticlesService
  @ApplicationScoped
  NOT @Transactional at class level
```

Each article is processed in a **separate transaction** (method annotated
`@Transactional(REQUIRES_NEW)` or delegated to a helper bean). This way:

- A gateway exception for article A does not roll back the already-committed
  save of article B.
- The specialties/vocabularies are loaded once **outside** any per-article
  transaction to avoid N+1 reads.

### Pseudocode

```
execute():
  if not enabled:
    LOG.info("Auto-specialty disabled by config — skipping.")
    return AutoAnalysisSummary(0,0,0,0)

  List<Article> candidates = articleRepo.findAllWithoutSpecialty()
  LOG.infof("Auto-specialty: %d articles to analyze", candidates.size())

  // Load specialties + vocabularies once
  List<Specialty> specialties = specialtyRepo.findAllActive()
  Map<UUID, Vocabulary> vocabs = buildVocabMap(specialties)
  List<SpecialtyDescriptor> descriptors = toDescriptors(specialties)

  int succeeded = 0, skipped = 0, failed = 0
  for article in candidates:
    if isBlank(titulo) && isBlank(abstract) && isBlank(keywords):
      LOG.warnf("Auto-specialty: article %s has no text — skipping", article.id)
      skipped++
      continue
    try:
      result = aiGateway.analyzeArticle(buildRequest(article, descriptors, vocabs))
      domainTags = result.tags().map(t -> ArticleTag.create(t.tipo(), t.valor()))
      saveArticleWithSpecialty(article, result.especialidadId(), domainTags)
      LOG.infof("Auto-specialty: article %s → specialty %s", article.id, especialidadNombre)
      succeeded++
    catch Exception e:
      LOG.errorf(e, "Auto-specialty: article %s failed: %s", article.id, e.getMessage())
      failed++

  LOG.infof("Auto-specialty pass complete: attempted=%d succeeded=%d skippedBlank=%d failed=%d",
            candidates.size(), succeeded, skipped, failed)
  return new AutoAnalysisSummary(candidates.size(), succeeded, skipped, failed)
```

### `saveArticleWithSpecialty` — isolated transaction helper

To isolate each article's save in its own transaction, a package-private
`@ApplicationScoped` helper bean is used:

```java
// application/pubmedautospecialty/ArticleSpecialtySaver.java
@ApplicationScoped
class ArticleSpecialtySaver {
    @Transactional(REQUIRES_NEW)
    void save(Article article, UUID especialidadId, List<ArticleTag> tags) {
        Article toSave = article.withAiAnalysis(especialidadId, tags);
        articleRepository.save(toSave);
    }
}
```

`AutoAnalyzeNewArticlesService` injects this saver. The gateway call happens
**outside** `save()` (no DB connection held during the HTTP call), which avoids
the long-held-connection anti-pattern that the manual analysis spec accepted for
single-article simplicity but is unacceptable for a 100-article batch.

## Scheduler modification (`infrastructure/pubmed/PubmedSyncScheduler`)

`PubmedSyncScheduler` is modified to inject and call
`AutoAnalyzeNewArticlesUseCase` after the sync use case completes:

```java
@Inject
AutoAnalyzeNewArticlesUseCase autoAnalyzeUseCase;

void syncPubmedArticles() {
    int count = syncUseCase.execute();
    LOG.infof("Sync: %d articles processed", count);
    AutoAnalysisSummary summary = autoAnalyzeUseCase.execute();
    LOG.infof("Auto-specialty: %s", summary);
}
```

The `onStart` path (startup sync) calls `syncPubmedArticles()` already, so the
auto-analysis fires at startup as well — no further change needed.

## Configuration

```properties
# application.properties
medsync.pubmed.auto-specialty.enabled=true
```

```properties
# %test.application.properties  (or in the test profile block)
%test.medsync.pubmed.auto-specialty.enabled=false
```

The test profile disables auto-specialty to prevent live Groq calls during
integration tests. Individual tests that need to verify the auto-analysis
behavior mock the gateway via `QuarkusMock`.

The `ai.groq.*` keys introduced in `12-05-2026-article-ai-analysis` are
inherited unchanged.

## Persistence

### New repository method implementation (`ArticleRepositoryImpl`)

```java
@Override
public List<Article> findAllWithoutSpecialty() {
    // JPQL — @SQLRestriction on the entity already filters soft-deleted rows
    return ArticleEntity
        .<ArticleEntity>list("especialidadId IS NULL ORDER BY createdAt ASC")
        .stream()
        .map(ArticlePersistenceMapper::toDomain)
        .toList();
}
```

No new SQL migration — `especialidad_id` column already exists (added in
`12-05-2026-specialty`).

> **Note on H2 compatibility**: `especialidadId IS NULL` in JPQL is H2-safe.
> The Panache `.list()` shorthand is already used in the repository impl per the
> project's existing patterns.

## Validation

| Level         | Check                                                                | Location                               |
|---------------|----------------------------------------------------------------------|----------------------------------------|
| App-level     | All text fields blank → skip (WARN log), no gateway call             | `AutoAnalyzeNewArticlesService`        |
| App-level     | `especialidad_id` already set → not in the candidate set (SQL filter)| `ArticleRepositoryImpl.findAllWithoutSpecialty()` |
| Config-level  | `medsync.pubmed.auto-specialty.enabled` defaults to `true` if absent | `@ConfigProperty(defaultValue="true")` |

No new domain exceptions are introduced — failures are caught, logged, and
counted without propagating.

## Exceptions

No new domain exceptions. All gateway exceptions (`AiAnalysisException`,
`AiAnalysisTimeoutException`) and any other runtime exceptions are caught in the
service loop's catch block and recorded as failures in `AutoAnalysisSummary`.

## Sequence flow

### Startup / cron trigger with auto-specialty enabled

```
Scheduler        SyncUseCase         PubmedClient/DB         AutoAnalyzeUseCase      AiGateway              DB
   │ onStart / cron  │                       │                       │                       │                 │
   │ syncPubmedArticles()                    │                       │                       │                 │
   │ ─────────────────►                      │                       │                       │                 │
   │                  │ execute()            │                       │                       │                 │
   │                  │ ─────────────────────► (fetch + upsert 100 articles)                │                 │
   │                  │ ◄── int count ───────│                       │                       │                 │
   │ ◄── count ───────│                      │                       │                       │                 │
   │ autoAnalyzeUseCase.execute()            │                       │                       │                 │
   │ ─────────────────────────────────────────────────────────────────►                     │                 │
   │                                                                  │ findAllWithoutSpecialty()              │
   │                                                                  │ ──────────────────────────────────────►│
   │                                                                  │ ◄── List<Article> ────────────────────│
   │                                                                  │ findAllActive() [specialties]         │
   │                                                                  │ getVocabularyFor(each)                │
   │                                                                  │                                       │
   │                                         for each article:        │                                       │
   │                                                                  │ analyzeArticle(request)               │
   │                                                                  │ ─────────────────────────────────────►│ (Groq POST ×2)
   │                                                                  │ ◄── ArticleAnalysisResult ───────────│
   │                                                                  │ saveArticleWithSpecialty(REQUIRES_NEW)│
   │                                                                  │ ──────────────────────────────────────►│ (UPDATE)
   │                                                                  │ [on exception: log ERROR, continue]   │
   │ ◄── AutoAnalysisSummary ─────────────────────────────────────────│                                       │
```

### Auto-specialty disabled

```
Scheduler        AutoAnalyzeUseCase
   │ autoAnalyzeUseCase.execute()
   │ ─────────────────────────────►
   │              │ enabled=false → LOG.info("disabled")
   │ ◄── AutoAnalysisSummary(0,0,0,0)
```

## Cross-feature impact

| Existing feature   | Affected artifact                          | Change                                                                       |
|--------------------|--------------------------------------------|------------------------------------------------------------------------------|
| `article`          | `ArticleRepository` (port)                 | Adds `findAllWithoutSpecialty()` method.                                     |
| `article`          | `ArticleRepositoryImpl` (impl)             | Implements `findAllWithoutSpecialty()` via JPQL.                             |
| `article`          | `ArticleRepositoryImplTest`                | New test cases for `findAllWithoutSpecialty()`.                              |
| `article-ai-analysis` | `AnalyzeArticleWithAiService`           | No code change — reused conceptually, not called directly.                   |
| `infrastructure/pubmed` | `PubmedSyncScheduler`               | Injects and calls `AutoAnalyzeNewArticlesUseCase` after sync.                |
| `application.properties` | config                             | Adds `medsync.pubmed.auto-specialty.enabled=true` and the `%test` override. |

## Key technical decisions

### 1. Post-sync, outside the sync transaction

The sync transaction commits article rows first. The auto-analysis pass runs
after. This ensures a slow or failing Groq response never rolls back PubMed data.
The cost: a narrow window where an article exists in the DB without a specialty.
The frontend already handles this (the card renders without color/icon when
`especialidadId` is null), so the window is cosmetically acceptable.

### 2. Per-article `REQUIRES_NEW` transaction

Each article's `save` uses `REQUIRES_NEW` so a gateway exception on article N
does not roll back the already-committed save of article N-1. The specialties
and vocabularies are loaded once outside the loop to avoid N+1 reads.

### 3. No reuse of `AnalyzeArticleWithAiService`

The batch service loads specialties/vocabularies once for all articles;
`AnalyzeArticleWithAiService` reloads them on every call. Calling the existing
service 100 times would incur 100 × `findAllActive()` + 100 × `getVocabularyFor()`
calls. The batch service avoids this by loading once and passing the preloaded
data to the gateway. This is the right trade-off for a batch context.

### 4. Sequential processing

Concurrent Groq calls would hit the per-minute token budget faster and complicate
error handling. Sequential processing is simpler and safe at 100-article scale.

### 5. No retry on 429

Retrying in a 100-article loop complicates the batch significantly and may
cause it to run for an indeterminate amount of time. A 429 is logged as a
failure; the operator can re-trigger the analysis for specific articles via the
existing `POST /api/articles/{id}/analyze` endpoint.

### 6. `findAllWithoutSpecialty` as a port method, not a service parameter

The list of unclassified articles is fetched inside the use case, not passed in
by the caller. This keeps the interface simple (`execute()` takes no arguments)
and makes the use case self-contained — the scheduler does not need to know about
the repository or how to build the candidate list.

### 7. Backfill of pre-existing unclassified articles

The query `WHERE especialidad_id IS NULL` naturally includes articles that were
imported before this feature shipped. No separate migration or manual step is
needed — the first post-deploy sync run will classify the full backlog.

## Open technical decisions / risks

- **Rate-limit 429 during a 100-article batch.** With ~200 Groq calls and a
  12k-token/min budget, a 100-article batch may saturate the free tier if articles
  have long abstracts. Mitigation in this spec: 429 → log ERROR → skip → continue.
  Follow-up spec may add a configurable delay between calls.
- **Startup latency.** The auto-analysis pass runs synchronously after the
  startup sync. If the startup sync fetches 100 new articles and each Groq call
  takes 2 s, the startup auto-analysis adds ~400 s (~7 min). The application is
  already serving traffic during this window (Quarkus startup completes before
  `onStart`). This is documented here for operator awareness; if startup latency
  is a concern, the `medsync.pubmed.sync.on-startup=false` flag disables the
  entire startup pass.
- **Article ordering.** `findAllWithoutSpecialty` orders by `createdAt ASC`
  so a restart continues from roughly where it left off (articles saved more
  recently appear later). This is best-effort — if a restart occurs mid-batch,
  already-saved articles are excluded by the `IS NULL` filter, so there is no
  risk of double-processing.
