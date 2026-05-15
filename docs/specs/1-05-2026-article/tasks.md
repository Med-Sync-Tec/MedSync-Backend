# Feature: Article — Implementation Tasks

> Status: **all tasks complete** except for AI tag extraction (deferred — see summary).

## Domain
- [x] `ArticleTest`, `ArticleTagTest` (constructor invariants, factories, withTagAdded/Removed) → RED
- [x] `Article`, `ArticleTag` POJOs
- [x] Domain exceptions: `ArticleNotFoundException`, `ArticleTagNotFoundException`, `DuplicateArticleException`, `InvalidArticleDataException`
- [x] `ArticleRepository` interface
- [x] 8 `*UseCase` interfaces
- [x] `TipoClinico` enum in `domain/shared/model/`
- [x] `Page<T>` record in `domain/shared/`

## Application
- [x] 8 `*ServiceTest` files (Mockito) → RED
- [x] 8 `*Service` implementations
- [x] `SyncPubmedArticlesService` orchestration (esearch → efetch → parse → upsert)

## Infrastructure — persistence
- [x] Migration `V6__create_articulos_cientificos.sql`
- [x] `ArticleRepositoryImplTest` (`@QuarkusTest` + `@TestTransaction`) → RED
- [x] `ArticleEntity` (`@NamedEntityGraph("Article.withTags")`)
- [x] `ArticleTagEntity`
- [x] `ArticlePersistenceMapper`
- [x] `ArticleRepositoryImpl`

## Infrastructure — PubMed
- [x] `PubmedEutilsClient` (REST client with timeouts)
- [x] `PubmedResponseParser` (XML parsing with XXE protection)
- [x] `PubmedArticleData` DTO
- [x] `PubmedParseException`
- [x] `PubmedSyncScheduler` (`@Scheduled` + `onStart`)
- [x] `%test.quarkus.scheduler.enabled=false` to keep test runs deterministic

## REST
- [x] `ArticleResourceIT` covering all 8 endpoints → RED
- [x] DTOs: `CreateArticleRequest`, `AddArticleTagRequest`, `ArticleResponse`, `ArticleTagResponse`, `PagedArticlesResponse`
- [x] `ArticleRestMapper`
- [x] `ArticleResource`
- [x] Wire new exceptions into `GlobalExceptionHandler`

## Verification
- [x] `./mvnw compile` clean
- [x] `./mvnw test` green
- [x] `./mvnw verify -DskipITs=false` green
- [x] Manual smoke test: `POST /api/articles/sync` returns a non-zero count when PubMed is reachable

## Deferred (out of scope for this iteration)
- [ ] `AiArticleTagGateway` interface in `domain/article/repository/`
- [ ] `AiArticleTagGatewayImpl` in `infrastructure/ai/`
- [ ] Wire AI extraction call into `SyncPubmedArticlesService.upsertArticle`
- [ ] Add config for the AI service (URL, API key, model name)
- [ ] Tests for AI extraction (with mocked gateway)
