# Feature: Database Objects (procedures, functions, triggers) — Requirements

## Overview

A small set of MySQL-side database objects that complement application-layer logic. The objects fix two latent bugs and demonstrate the procedural-SQL patterns required by the course rubric (procedures, functions, triggers). They are **MySQL-only**: H2 in `MODE=MySQL` does not support the full procedural syntax (DELIMITER, SIGNAL, multi-statement triggers with BEGIN…END), so the Flyway migration that creates them lives under a vendor-specific path and is skipped on H2.

This snapshot ships **the first three objects** of the eventual rubric requirement (2 procedures + 2 functions + 4 triggers = 8 objects). The remaining five (1 procedure, 2 functions, 3 triggers) are deferred to a follow-up spec under the same convention.

## Course-requirement scope

| Object kind | Required total | Shipped in this snapshot | Deferred |
|-------------|----------------|--------------------------|----------|
| Procedures  | 2              | 1                        | 1        |
| Functions   | 2              | 0                        | 2        |
| Triggers    | 4              | 2 (one is split into 3 MySQL-level triggers — one logical trigger) | 2 |

## Objects shipped in this snapshot

### Trigger 1 — `tr_article_tags_bump_updated_at_{ins,upd,del}`

**Bug it fixes**: Hibernate's `@UpdateTimestamp` only watches direct entity updates. When AI tag replacement (feature 3) cascades a `DELETE old + INSERT new` on `articulo_tags`, the parent `articulos_cientificos.updated_at` is **not** bumped. The matching query orders by `a.updated_at DESC`, so AI-tagged articles would not surface as "recently updated" even though they just changed.

**Behavior**: After any INSERT, UPDATE, or DELETE on `articulo_tags`, the parent article's `updated_at` is set to `CURRENT_TIMESTAMP`. MySQL does not allow one trigger to bind multiple events, so this is implemented as three sibling triggers that share a name prefix.

### Trigger 2 — `tr_specialty_soft_delete_guard`

**Bug it fixes**: Today the application can flip `especialidades.activo = false` while articles or `paciente_contexto` rows still carry that specialty's id in their `especialidad_id` FK. The FK constraint is still satisfied (the row exists), but the matching query stops returning those articles because `@SQLRestriction` hides the parent row in JPA. The data is dangling.

**Behavior**: `BEFORE UPDATE` on `especialidades`. If a row is transitioning from `activo = TRUE` to `activo = FALSE` and any article or `paciente_contexto` row still references it, the trigger raises `SQLSTATE 45000` with a clear message and the UPDATE is aborted. Other UPDATEs (renames, descriptions, re-activations) are unaffected.

### Procedure 1 — `sp_match_articles_for_patient(p_patient_id, p_limit)`

**Why it exists**: encapsulates the patient-matching query that `ArticleRepositoryImpl.findMatchingArticlesForPaciente` currently runs as inline JPQL. The procedure can be called directly from MySQL Workbench during demos (no Java involvement), and serves as the canonical version of the query when feature 3 adds the `AND c.especialidad_id = a.especialidad_id` join clause.

**Behavior**: returns the distinct articles whose tags overlap a patient's clinical context, ordered by `updated_at DESC` and capped by the caller-supplied `LIMIT`.

## User stories

- As the **course rubric**, I require the project to demonstrate ≥ 2 procedures, ≥ 2 functions, and ≥ 4 triggers, so that students show fluency in procedural SQL beyond plain DDL.
- As a **developer maintaining the matching pipeline**, I want article `updated_at` to reflect the time of the most recent tag change (manual or AI-driven), so that "recently updated" ordering is correct.
- As an **operator**, I want to be prevented from soft-deleting a specialty that still has active references, so that I do not silently break the matching query for users tagged with that specialty.
- As an **analyst running ad-hoc queries**, I want a stored procedure that performs the patient-matching query, so that I can call it from MySQL Workbench without copying the JPQL into raw SQL by hand.

## Acceptance criteria (EARS format)

### Ubiquitous

- The system shall apply migration `V13__add_database_objects.sql` against MySQL data sources only. H2 boots (dev and `@QuarkusTest`) shall not apply it — the new objects are absent on H2.
- The system shall preserve the existing application behavior on H2: the inline JPQL matching query, the cascaded tag replacement via Hibernate, and the soft-delete on `especialidades` continue to work without the triggers.

### Event-driven (triggers)

- When a row is inserted into `articulo_tags`, the system shall set `articulos_cientificos.updated_at = CURRENT_TIMESTAMP` for the row whose `id = NEW.articulo_id`.
- When a row in `articulo_tags` is updated, the system shall set `articulos_cientificos.updated_at = CURRENT_TIMESTAMP` for the row whose `id = NEW.articulo_id`.
- When a row in `articulo_tags` is deleted, the system shall set `articulos_cientificos.updated_at = CURRENT_TIMESTAMP` for the row whose `id = OLD.articulo_id`.
- When an UPDATE on `especialidades` transitions `activo` from `TRUE` to `FALSE` while any `articulos_cientificos.especialidad_id` or `paciente_contexto.especialidad_id` references the row, the system shall abort the UPDATE with `SQLSTATE 45000` and a message naming the conflict.

### Conditional (procedure)

- If `CALL sp_match_articles_for_patient(p_patient_id, p_limit)` runs against a patient with at least one `paciente_contexto` row whose `(tipo, valor)` matches a tag of at least one article, the procedure shall return those articles ordered by `updated_at DESC` and capped at `p_limit` rows.
- If the patient has no contexts or no overlapping tags, the procedure shall return zero rows.
- If `p_limit` is `0` or negative, the procedure's behavior follows MySQL's `LIMIT` semantics for that value (no special handling).

### State-driven

- While the inline JPQL matching query in `ArticleRepositoryImpl` is still the primary code path, the procedure and that query shall be **kept in sync** when feature 3 adds the specialty-join clause. The procedure documentation notes this maintenance link.

## Non-functional requirements

- **Cross-feature touch**: this migration depends on tables introduced by features `article` (V6), `patient-context` (V5), and `specialty` (V10/V11/V12). It does not introduce new tables.
- **Reversibility**: each `CREATE TRIGGER` and `CREATE PROCEDURE` is preceded by a `DROP … IF EXISTS` so the migration is idempotent if Flyway ever re-runs it after a baseline reset.
- **No data backfill**: the triggers fire only on **future** writes. Existing rows whose `updated_at` is stale will only be corrected on the next tag write to their article. This is acceptable — the matching query was already working on stale timestamps before.

## Out of scope (explicit)

- **H2-compatible parity**. Translating these objects to H2's Java-callback trigger syntax is deferred. Tests that need to verify trigger behavior run against the `mysql-local` profile or a Testcontainers MySQL instance.
- **The remaining 5 objects** (1 procedure, 2 functions, 3 triggers) needed to fully satisfy the course rubric. They land in a follow-up snapshot.
- **A `quarkus-test-containers` dependency**. Adding Testcontainers for MySQL integration testing is a separate decision; this spec leaves test coverage of the new objects as manual smoke testing under `mysql-local`.
- **Replacing the inline JPQL query** in `ArticleRepositoryImpl` with a `CALL sp_match_articles_for_patient`. The procedure is a parallel, demonstrable artifact, not a replacement — replacing the inline query would break H2 dev.

## Open questions

- Should the soft-delete guard offer an "auto-NULL the references" mode instead of rejecting? *Not blocking — rejecting forces a deliberate operator decision, which is the safer default. Revisit if the COO complains.*
- Should we add a Java-backed H2 equivalent so the triggers fire in `@QuarkusTest` runs? *Not blocking — the application layer was correct enough before the triggers existed, and tests run against H2 today without them. Revisit when adding Testcontainers becomes worthwhile.*
