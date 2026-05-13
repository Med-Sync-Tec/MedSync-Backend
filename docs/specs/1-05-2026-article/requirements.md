# Feature: Article — Requirements

## Overview

Scientific articles + their clinical tags + automated PubMed synchronization. Articles power the matching algorithm that connects new evidence to patients with relevant clinical context. Tags carry the matching key: each tag is a `(TipoClinico, valor)` pair, mirroring the `paciente_contexto` schema so a SQL-level join can find articles relevant to a patient.

## User stories

- As a **doctor**, I want to browse recently updated scientific articles, so that I stay informed of new evidence.
- As a **doctor**, I want to view an article's full metadata and tags, so that I can decide if it's clinically relevant.
- As a **doctor**, I want to see articles that match the clinical context of one of my patients, so that I can apply new evidence to their care.
- As a **content curator**, I want to add or remove tags on an article, so that the matching algorithm uses correct keywords.
- As a **content curator**, I want to create an article manually (when PubMed sync misses it), so that the catalog stays complete.
- As the **system**, I want to ingest the most recent and most trending PubMed articles on a daily basis, so that the catalog stays fresh without manual work.

## Acceptance criteria (EARS format)

### Ubiquitous
- The system shall enforce a global `UNIQUE` constraint on `doi`. An article can be re-imported from PubMed without producing duplicates.
- The system shall store article tags as `(tipo, valor)` pairs, where `tipo` is one of `ENFERMEDAD`, `SINTOMA`, `TRATAMIENTO`, `MEDICAMENTO`.
- The system shall enforce the same `TipoClinico` enum across `article_tag` and `paciente_contexto` so the matching query is a straight join.

### Event-driven
- When a valid create-article request is received, the system shall return `201` with the article and a `Location` header.
- When a get-article-by-id request is received, the system shall return `200` with the article **and all its tags eagerly loaded**.
- When a list-articles request is received, the system shall return `200` with a paginated list ordered by `id ASC` (stable pagination).
- When a get-recent-articles request is received, the system shall return `200` with articles ordered by `updatedAt DESC, id ASC`.
- When a sync request is received (`POST /api/articles/sync`), the system shall query PubMed E-utilities, fetch up to 100 articles, upsert by DOI (fallback to URL), and return `{"articulosProcesados": <count>}`.
- When the daily scheduler fires (12:00 UTC), the system shall run the same sync logic.
- When the application starts, the system shall trigger an initial sync (best-effort, errors logged but not fatal).
- When an add-tag request is received for an existing article, the system shall return `201` with the new tag.
- When a remove-tag request is received for an existing article and tag, the system shall return `204`.
- When a `GET /api/patients/{patientId}/matching-articles` request is received, the system shall:
  - Verify the patient exists (else `404`).
  - Join `paciente_contexto` ⨯ `articulo_tags` on `(tipo, valor)`.
  - Return the matching articles, capped at `limit` (default 50, max 100).

### Conditional
- If `doi` already exists on a create, the system shall return `409`.
- If a get / add-tag / remove-tag request references a non-existent article id, the system shall return `404`.
- If a remove-tag request references a non-existent tag id, the system shall return `404`.
- If `anioPub` is outside the range `1800..currentYear+1`, the system shall return `400`.
- If pagination size exceeds 100, the system shall clamp to 100.

## Non-functional requirements

- **Throughput**: PubMed sync of 100 articles must finish in under 60s under normal network conditions.
- **Stability**: PubMed errors (network, parsing) must not crash the scheduler — log and continue.
- **Compatibility**: parser must tolerate missing fields in PubMed XML (PubMed routinely omits fields) — fill nulls with the literal string `"No disponible"`.
- **Idempotency**: re-running sync must not duplicate articles (DOI is the primary uniqueness key; URL is the fallback).

## Out of scope (explicit)

- AI-driven tag extraction. *(The `infrastructure/ai/` package exists as a placeholder.)*
- Full-text article storage. We store metadata + abstract only.
- Searching by free-text query.
- Editing article metadata after creation.
- Tag taxonomy beyond the four `TipoClinico` values.

## Open questions

- When AI tagging lands, where does it hook in — at PubMed import time, or asynchronously after persistence? *Tracked in summary as deferred design.*
- Should `GetMatchingArticlesByPatient` paginate? Currently it returns a list capped at 100. Revisit when patient context grows.
