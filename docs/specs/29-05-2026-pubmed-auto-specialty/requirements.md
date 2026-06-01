# Feature: PubMed Auto-Specialty — Requirements

## Overview

When articles are pulled from PubMed (both the initial 100-article seed and any
subsequent daily delta), the system automatically classifies each new article
into one of MedSync's medical specialties using the existing AI analysis
pipeline (`AiAnalysisGateway`). Articles that already have a `especialidad_id`
are skipped to avoid redundant AI calls. This removes the manual "Analyze with
AI" step as a prerequisite for specialty assignment, so the frontend card
color/icon renders correctly from the moment an article first appears.

## User stories

- As a **doctor**, I want every newly imported PubMed article to already have a
  specialty assigned, so that it shows the correct color and icon in the feed
  without me having to click "Analyze with AI" first.
- As an **operator**, I want the system to never re-analyze articles that
  already have a specialty, so that we stay within Groq's free-tier rate limits
  and avoid unnecessary spend.
- As an **operator**, I want the auto-analysis to run asynchronously (after
  articles are persisted), so that a slow Groq response never delays the sync
  transaction or causes it to roll back.
- As an **operator**, I want any auto-analysis failure on a single article to be
  logged and skipped rather than aborting the whole batch, so that one bad
  article does not block the rest.
- As an **operator**, I want the feature to be toggleable via configuration, so
  that it can be disabled during testing or when the Groq quota is exhausted.

## Acceptance criteria (EARS format)

### Ubiquitous

- The system shall skip auto-analysis for any article that already has a
  non-null `especialidad_id` at the time the sync runs.
- The system shall use the same `AiAnalysisGateway.analyzeArticle` path already
  used by the manual `POST /api/articles/{id}/analyze` endpoint — no separate
  AI integration.
- The system shall log the outcome (success, skip, failure) for each article at
  INFO level, including the article id and, on success, the assigned specialty
  name.

### Event-driven

- When a PubMed sync completes (triggered either by startup or by the daily
  cron), the system shall enqueue all newly-persisted articles (those with
  `especialidad_id = NULL`) for specialty analysis.
- When an article is enqueued for analysis and has at least one non-blank field
  among `titulo`, `abstractText`, and `keywords`, the system shall call
  `AiAnalysisGateway.analyzeArticle` and persist the returned `especialidadId`
  and tag list onto the article using `Article.withAiAnalysis`.
- When an article is enqueued for analysis and **all** of its `titulo`,
  `abstractText`, and `keywords` are blank/null, the system shall skip it,
  log a WARN entry, and continue with the next article without throwing.
- When `AiAnalysisGateway.analyzeArticle` throws any exception for a given
  article, the system shall log an ERROR entry (including the article id and the
  exception message) and continue processing remaining articles — the failure
  must not abort the batch.

### State-driven

- While `medsync.pubmed.auto-specialty.enabled` is `false`, the system shall
  skip the entire auto-analysis pass after each sync and log a single INFO entry
  stating that auto-specialty is disabled.
- While the Groq free-tier rate limit (HTTP 429) is hit, the system shall log
  an ERROR for the affected article (same as any other gateway exception) and
  continue with the next one; **no automatic retry** is performed in this spec.

### Conditional

- If the article's `especialidad_id` is already set at the point the
  auto-analysis pass runs, the system shall not call the gateway for that
  article.
- If `medsync.pubmed.auto-specialty.enabled` is missing from
  `application.properties`, the system shall treat it as `true` (enabled by
  default).

## Non-functional requirements

- **Decoupling**: the auto-analysis pass runs **outside** the sync transaction.
  The sync transaction commits its insertions first; the analysis pass runs
  afterwards. A failure in the analysis pass must not roll back the persisted
  articles.
- **Sequential processing**: articles are analyzed one at a time (no parallel
  HTTP calls to Groq) to respect the free-tier rate limit (~30 req/min,
  ~12k tokens/min). No artificial sleep is added in this spec; if rate limiting
  is hit, the article is skipped (see above). A configurable throttle may be
  added in a follow-up spec.
- **Rate-limit awareness**: given the Groq free-tier budget (~30 req/min), a
  batch of 100 new articles makes ~200 HTTP calls (2 per article). At
  typical Groq latency (1–3 s per call), 100 articles takes roughly 4–10
  minutes. This is acceptable for a background pass.
- **Configurability**: `medsync.pubmed.auto-specialty.enabled` is the only new
  config key introduced. All Groq parameters (`ai.groq.*`) are inherited from
  the `article-ai-analysis` spec.
- **Observability**: at the end of each auto-analysis pass the system shall log
  a summary at INFO level: total articles attempted, succeeded, skipped
  (already analyzed), and failed.

## Out of scope (explicit)

- **Retry logic with backoff** for 429s — a follow-up spec may add exponential
  backoff with `Retry-After` support.
- **Parallel/concurrent analysis** — sequential only in this spec.
- **Re-analyzing already-classified articles** — the filter `especialidad_id IS
  NULL` is intentional. A "re-tag all" bulk operation is a separate future spec.
- **Manual trigger endpoint** for the auto-analysis pass — the existing
  `POST /api/articles/{id}/analyze` already serves that need per article.
- **Vocabulary auto-creation** for specialties that have no vocabulary — the
  same behavior as the manual analysis spec applies (a 502 is logged as an
  ERROR, the article is skipped).
- **Persisting the analysis event log** in the database — outcomes are logged
  only, not stored as rows.
- **Notification to the doctor** (WebSocket push, etc.) when the batch completes
  — out of scope for this backend spec.

## Open questions

- [ ] Should there be a configurable delay (e.g. `medsync.pubmed.auto-specialty.delay-between-articles`) between Groq calls to stay safely under the rate limit? *Not blocking — sequential calls already add natural latency; if 429s become common, a follow-up spec adds the delay.*
- [ ] Should the auto-analysis pass also re-analyze articles from previous syncs that are still unclassified (i.e., articles already in the DB before this feature ships)? *Decision: yes — the pass queries `especialidad_id IS NULL` across all existing articles, not just those from the latest sync batch. This is the simplest backfill strategy and requires no migration.*
