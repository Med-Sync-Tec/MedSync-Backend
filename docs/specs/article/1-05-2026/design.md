# Feature: Article — Design

## Domain models

### `Article` (immutable, `domain/article/model/Article.java`)

| Field             | Type                | Required | Notes                                              |
|-------------------|---------------------|----------|----------------------------------------------------|
| id                | `UUID`              | yes      | server-generated                                   |
| titulo            | `String`            | yes      | 1..1000 chars                                      |
| autores           | `String`            | no       | free text, TEXT column                             |
| revista           | `String`            | no       | up to 500 chars                                    |
| anioPub           | `Integer`           | no       | `1800..currentYear+1`                              |
| mesPub            | `String`            | no       | up to 20 chars                                     |
| doi               | `String`            | no       | up to 200 chars, **unique** when present           |
| abstractText      | `String`            | no       | TEXT                                               |
| keywords          | `String`            | no       | TEXT                                               |
| tipoPublicacion   | `String`            | no       | up to 100 chars                                    |
| url               | `String`            | no       | up to 500 chars                                    |
| tags              | `List<ArticleTag>`  | yes      | always non-null; copy-on-write                     |
| createdAt         | `LocalDateTime`     | auto     |                                                    |
| updatedAt         | `LocalDateTime`     | auto     |                                                    |

Behaviors:
- `Article.create(...)` — generates UUID and initializes `tags = []`.
- `Article.withTagAdded(tag)` — returns a new `Article` with the tag appended.
- `Article.withTagRemoved(tagId)` — returns a new `Article` with the tag removed.

### `ArticleTag` (immutable value object)

| Field      | Type             | Required | Notes                                       |
|------------|------------------|----------|---------------------------------------------|
| id         | `UUID`           | yes      |                                             |
| tipo       | `TipoClinico`    | yes      | enum `ENFERMEDAD | SINTOMA | TRATAMIENTO | MEDICAMENTO` |
| valor      | `String`         | yes      | 1..500 chars                                |
| createdAt  | `LocalDateTime`  | auto     | assigned at persistence time                |

`TipoClinico` lives in `domain/shared/model/` because it is shared with `patient-context`.

## Output port

```java
public interface ArticleRepository {
    Article save(Article article);
    Optional<Article> findByUuid(UUID id);
    Page<Article> findAllPaged(int page, int size);
    Page<Article> findRecentPaged(int page, int size);
    boolean existsByDoi(String doi);
    boolean existsByUrl(String url);
    List<Article> findMatchingByPatientContext(UUID patientId, int limit);
}
```

`Page<T>` is a thin record `(List<T> content, int page, int size, long totalElements, int totalPages)` (lives in `domain/shared/`).

## Use cases

| Use case interface                       | Service                                   | Trigger / endpoint                          |
|------------------------------------------|-------------------------------------------|---------------------------------------------|
| `CreateArticleUseCase`                   | `CreateArticleService`                    | `POST /api/articles`                        |
| `GetArticleByIdUseCase`                  | `GetArticleByIdService`                   | `GET /api/articles/{id}`                    |
| `ListArticlesUseCase`                    | `ListArticlesService`                     | `GET /api/articles`                         |
| `GetRecentArticlesUseCase`               | `GetRecentArticlesService`                | `GET /api/articles/recent`                  |
| `AddTagToArticleUseCase`                 | `AddTagToArticleService`                  | `POST /api/articles/{id}/tags`              |
| `RemoveTagFromArticleUseCase`            | `RemoveTagFromArticleService`             | `DELETE /api/articles/{id}/tags/{tagId}`    |
| `GetMatchingArticlesByPatientUseCase`    | `GetMatchingArticlesByPatientService`     | `GET /api/patients/{patientId}/matching-articles` |
| `SyncPubmedArticlesUseCase`              | `SyncPubmedArticlesService`               | `POST /api/articles/sync` + scheduler       |

## REST endpoints

| Method | Path                                            | Request DTO              | Response DTO                  | Status codes |
|--------|-------------------------------------------------|--------------------------|-------------------------------|--------------|
| POST   | `/api/articles`                                  | `CreateArticleRequest`   | `ArticleResponse`             | 201 / 400 / 409 |
| GET    | `/api/articles`                                  | `?page&size`             | `PagedArticlesResponse`       | 200          |
| GET    | `/api/articles/{id}`                             | —                        | `ArticleResponse` (+ tags)    | 200 / 404    |
| GET    | `/api/articles/recent`                           | `?page&size`             | `PagedArticlesResponse`       | 200          |
| POST   | `/api/articles/sync`                             | —                        | `{ "articulosProcesados": int }` | 200       |
| POST   | `/api/articles/{id}/tags`                        | `AddArticleTagRequest`   | `ArticleTagResponse`          | 201 / 404    |
| DELETE | `/api/articles/{id}/tags/{tagId}`                | —                        | —                             | 204 / 404    |
| GET    | `/api/patients/{patientId}/matching-articles`    | `?limit`                 | `List<ArticleResponse>`       | 200 / 404    |

Swagger tag: `Articles`.

### DTOs (`interfaces/rest/article/`)
- `CreateArticleRequest` — Jakarta validation on `titulo` (`@NotBlank @Size(max=1000)`), `doi` (`@Size(max=200)`), `url` (`@Size(max=500)`), etc.
- `AddArticleTagRequest` — `tipo` (`@NotBlank @Size(max=30)`), `valor` (`@NotBlank @Size(max=500)`).
- `ArticleResponse` — record with all metadata + nested `List<ArticleTagResponse>`.
- `PagedArticlesResponse` — record `(content, page, size, totalElements, totalPages)`.

## Persistence

### `ArticleEntity` (table `articulos_cientificos`)

| Column              | Type            | Notes                                  |
|---------------------|-----------------|----------------------------------------|
| id                  | `BINARY(16)`    | PK                                     |
| titulo              | `VARCHAR(1000)` | NOT NULL                               |
| autores             | `TEXT`          |                                        |
| revista             | `VARCHAR(500)`  |                                        |
| anio_pub            | `INT`           |                                        |
| mes_pub             | `VARCHAR(20)`   |                                        |
| doi                 | `VARCHAR(200)`  | UNIQUE (nullable, MySQL allows multiple NULL) |
| abstract_text       | `TEXT`          |                                        |
| keywords            | `TEXT`          |                                        |
| tipo_publicacion    | `VARCHAR(100)`  |                                        |
| url                 | `VARCHAR(500)`  |                                        |
| created_at          | `TIMESTAMP`     | auto                                   |
| updated_at          | `TIMESTAMP`     | auto                                   |

Index: `idx_articulos_anio (anio_pub)`.

### `ArticleTagEntity` (table `articulo_tags`)

| Column        | Type           | Notes                                                  |
|---------------|----------------|--------------------------------------------------------|
| id            | `BINARY(16)`   | PK                                                     |
| articulo_id   | `BINARY(16)`   | NOT NULL, FK → `articulos_cientificos(id)`             |
| tipo          | `VARCHAR(30)`  | NOT NULL, enum as String                               |
| valor         | `VARCHAR(500)` | NOT NULL                                               |
| created_at    | `TIMESTAMP`    | auto                                                   |

Indexes:
- `idx_articulo_tags_articulo (articulo_id)`
- `idx_articulo_tags_lookup (articulo_id, tipo, valor)` — speeds tag CRUD
- `idx_articulo_tags_match (tipo, valor)` — speeds the patient-match join

### Relationships

- `ArticleEntity` ⟶ `ArticleTagEntity` is `@OneToMany(mappedBy="articulo", cascade=ALL, orphanRemoval=true, fetch=LAZY)`.
- `ArticleTagEntity` ⟶ `ArticleEntity` is `@ManyToOne(fetch=LAZY)`.
- `@NamedEntityGraph("Article.withTags")` — fetch graph for `getArticleById` and matching queries.

### Migration

`V6__create_articulos_cientificos.sql` — both tables + indexes.

## PubMed integration

Code lives in `infrastructure/pubmed/`:

- `PubmedEutilsClient` — wraps the two HTTP calls:
  - `esearch.fcgi` with term `(trending[sb]) OR (2024:2025[pdat] AND medicine[all])`, `retmax=100`.
  - `efetch.fcgi` in batches of 20 PMIDs (XML response).
- `PubmedResponseParser` — XML parser (with XXE protection) that extracts title, authors, journal, year, DOI, abstract, MeSH terms, URL.
- `PubmedArticleData` — DTO with all extracted fields.
- `PubmedSyncScheduler` — cron `0 0 12 * * ?` (daily 12 UTC) **and** `onStart` for initial backfill. Disabled in `%test` via `%test.quarkus.scheduler.enabled=false`.
- `PubmedParseException` — wraps `SAXException`, `JSONException`, etc.

Sync flow in `SyncPubmedArticlesService`:

1. `esearch` → up to 100 PMIDs.
2. `efetch` in batches of 20 → XML → `List<PubmedArticleData>`.
3. For each: check `existsByDoi(doi)`. If yes, skip. If no DOI, check `existsByUrl(url)`. If yes, skip.
4. Build `Article` + initial tags (if any can be inferred from MeSH today — currently empty list; AI hook lives here).
5. `save`.
6. Return processed count.

## `GetMatchingArticlesByPatient` algorithm

```sql
SELECT DISTINCT a.*
FROM articulos_cientificos a
JOIN articulo_tags t   ON t.articulo_id = a.id
JOIN paciente_contexto c ON c.tipo = t.tipo AND c.valor = t.valor
WHERE c.paciente_id = :patientId
ORDER BY a.updated_at DESC
LIMIT :limit
```

Verifies patient exists first (`PatientRepository.findByUuid`) — throws `PatientNotFoundException` if not.

## Validation

| Level     | Check                                                       | Location                          |
|-----------|-------------------------------------------------------------|-----------------------------------|
| Format    | `@NotBlank @Size(max=1000)` on `titulo`                     | `CreateArticleRequest`            |
| Format    | `@Size(max=200/500/20/100)` on optional fields              | `CreateArticleRequest`            |
| Format    | `@NotBlank @Size(max=30/500)` on `tipo`, `valor`            | `AddArticleTagRequest`            |
| Business  | `titulo` not blank                                          | `Article` constructor             |
| Business  | `anioPub` in `1800..currentYear+1`                          | `Article` constructor             |
| Business  | Tag `valor` 1..500, `tipo` non-null                         | `ArticleTag` constructor          |
| App-level | `doi` unique on create                                      | `CreateArticleService` + DB UNIQUE |
| App-level | `doi` / `url` uniqueness on PubMed upsert                   | `SyncPubmedArticlesService`       |

## Exceptions

| Domain exception                  | HTTP |
|-----------------------------------|------|
| `ArticleNotFoundException`        | 404  |
| `ArticleTagNotFoundException`     | 404  |
| `DuplicateArticleException`       | 409  |
| `InvalidArticleDataException`     | 400  |
| `PubmedParseException`            | 500 (logged, sync continues)  |

## Key technical decisions

### 1. `OneToMany` (not `ManyToMany`) for tags

Tags belong to a single article. Two articles can have the same `(tipo, valor)` but they are separate rows. This simplifies schema (no join table) and lets the matching query be a direct join on the indexed `(tipo, valor)` columns.

### 2. `orphanRemoval = true` + `cascade = ALL`

`Article.withTagRemoved(...)` mutates the in-memory list, then `save` persists; orphan removal deletes the row. No separate `removeTag` call against the entity manager.

### 3. `@NamedEntityGraph("Article.withTags")`

Eager fetch for endpoints that need tags (`getArticleById`, matching). The list endpoint does not use it — listing 100 articles + 1000 tags is wasteful when only summaries are shown. Per [conventions/persistence.md](../../conventions/persistence.md): never EAGER, always graph.

### 4. PubMed `retmax = 100`

A previous attempt used 1000 — caused E-utils rate-limit issues without an API key. 100 covers daily "trending + recent" cadence comfortably (~6 fresh items / day in observed runs).

### 5. `efetch` batch size = 20

XML responses for 20 PMIDs are ~2-3 MB. At 100 PMIDs Python E-utils returns >15 MB and frequently times out. 20 is empirically stable.

### 6. Upsert key: DOI primary, URL fallback

DOIs are immutable identifiers. URLs change (PubMed redirects, mirrors). DOI is the right unique key. URL is used only when DOI is missing.

### 7. Fill nulls with `"No disponible"`

Frontend simplification — no per-field null checks. Trade-off: indistinguishable from an actual `"No disponible"` string. Acceptable today; flagged for review when frontend introduces empty-state handling.

### 8. AI tag extraction hook reserved at `SyncPubmedArticlesService.upsertArticle`

When implemented, an `AiArticleTagGateway.extractTags(PubmedArticleData) → List<ArticleTag>` will be called after parsing and before save. Hook point identified but not yet wired. See `1-05-2026/summary.md` for deferred work.

### 9. Scheduler runs at startup + daily

`onStart()` triggers a sync on app boot so first-time deployments populate immediately. Subsequent invocations follow the cron schedule. Both call the same service.

### 10. Tag matching uses `paciente_contexto`, not `HospitalGateway`

The matching is MedSync-internal — `paciente_contexto` is the canonical place for tagged clinical context. We do not parse hospital consultations to derive tags at query time (too slow, hospital DB load).
