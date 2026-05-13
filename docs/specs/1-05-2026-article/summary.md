# Feature: Article — Implementation Summary

## What shipped

CRUD-minus-update for scientific articles, tag management, and a scheduled daily sync from PubMed E-utilities. Patient-match endpoint joins `paciente_contexto` ⨯ `articulo_tags` to surface relevant evidence per patient. AI-driven tag extraction is **not yet wired** — placeholder hook documented for the next iteration.

## Endpoints delivered

| Method | Path                                            | Status codes |
|--------|-------------------------------------------------|--------------|
| POST   | `/api/articles`                                 | 201 / 400 / 409 |
| GET    | `/api/articles`                                 | 200          |
| GET    | `/api/articles/{id}`                            | 200 / 404    |
| GET    | `/api/articles/recent`                          | 200          |
| POST   | `/api/articles/sync`                            | 200          |
| POST   | `/api/articles/{id}/tags`                       | 201 / 404    |
| DELETE | `/api/articles/{id}/tags/{tagId}`               | 204 / 404    |
| GET    | `/api/patients/{patientId}/matching-articles`   | 200 / 404    |

## Migrations applied

- `V6__create_articulos_cientificos.sql`

## Deviations from design

- **`retmax` lowered from 1000 to 100** mid-implementation due to PubMed rate-limit responses without an API key. Documented in commit `767f9ec`.

## Deferred from scope

- **AI-based tag extraction** — empty `infrastructure/ai/` package reserved. Hook point identified in `SyncPubmedArticlesService.upsertArticle()`. See `1-05-2026/tasks.md` deferred section.
- **Free-text article search** — not supported. Use `GET /matching-articles` for clinical-context queries.
- **Full-text article storage** — only metadata + abstract.
- **Edit endpoint** for articles.
- **Pagination on `matching-articles`** — capped list only.

## Acknowledged technical debt

1. **PubMed sync runs on app startup** (`onStart`) without backoff. Cold starts after an outage may pile requests on E-utils.
2. **`PubmedParseException` is logged and the batch continues.** A poisoned PMID drops one article — acceptable but no metrics yet.
3. **Null PubMed fields are filled with the literal `"No disponible"`** — indistinguishable from a real "No disponible" string in queries.
4. **No retries on PubMed timeout** (`30s read, 10s connect`). A flaky network burns a daily sync.
5. **`onStart` sync runs in non-test profiles only** — disabled in `%test` via scheduler config.

## Tests delivered

| File                                       | Type                          | What it covers                                       |
|--------------------------------------------|-------------------------------|------------------------------------------------------|
| `ArticleTest`, `ArticleTagTest`            | unit                          | Invariants, factories, withTagAdded/Removed          |
| `CreateArticleServiceTest` …               | unit (Mockito)                | Per-service behavior, exception propagation          |
| `SyncPubmedArticlesServiceTest`            | unit (Mockito)                | Upsert by DOI, by URL, parse failure swallowed       |
| `PubmedResponseParserTest`                 | unit                          | Real fixture XML, missing fields, XXE protection     |
| `ArticleRepositoryImplTest`                | `@QuarkusTest`                | EntityGraph, FK cascade, orphan removal              |
| `ArticleResourceIT`                        | RestAssured                   | All 8 endpoints + status codes + pagination edges    |

## Features that depend on this one

- **`alert`** (planned) — alerts surface when a matched article's tags align with a patient's context **and** the article is recent.
- **`patient-context`** — provides the join key (`(tipo, valor)`) for `getMatchingArticlesByPatient`.
- **AI extraction module** (future) — will populate tags automatically at sync time.
