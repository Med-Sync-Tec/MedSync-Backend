# Feature: Database Objects — Design

> Spec name: `database-objects`. No Java code; this snapshot is entirely Flyway-side. Vendor-scoped under `db/vendor_mysql/`.

## Flyway layout change

Quarkus's Flyway integration discards any sub-location that overlaps a parent location, so `db/migration` and `db/migration/{vendor}` cannot coexist. The vendor-specific folder must be a **sibling** of the base migration folder. Layout after this snapshot:

```
src/main/resources/db/
├── migration/                 ← vendor-agnostic migrations (V1–V12 today)
│   ├── V1__create_patients_table.sql
│   ├── …
│   └── V12__add_especialidad_id_and_drop_legacy.sql
├── vendor_mysql/              ← MySQL-only migrations
│   └── V13__add_database_objects.sql
└── vendor_h2/                 ← H2-only migrations (currently empty)
```

`application.properties` is configured as:

```properties
quarkus.flyway.locations=db/migration,db/vendor_{vendor}
```

The `{vendor}` placeholder resolves to `mysql` or `h2` based on the active datasource kind. Flyway scans both locations and applies migrations in version order; an absent V13 in `vendor_h2/` simply means H2 never sees the procedure or triggers.

## Object inventory

| Object                                            | Kind      | Created in | Drops on re-run | Body shape                              |
|---------------------------------------------------|-----------|------------|-----------------|-----------------------------------------|
| `tr_article_tags_bump_updated_at_ins`             | Trigger   | V13        | yes             | single-statement UPDATE                  |
| `tr_article_tags_bump_updated_at_upd`             | Trigger   | V13        | yes             | single-statement UPDATE                  |
| `tr_article_tags_bump_updated_at_del`             | Trigger   | V13        | yes             | single-statement UPDATE                  |
| `tr_specialty_soft_delete_guard`                  | Trigger   | V13        | yes             | multi-statement (DELIMITER, IF, SIGNAL)  |
| `sp_match_articles_for_patient(BINARY(16), INT)`  | Procedure | V13        | yes             | single SELECT inside DELIMITER block     |

## Trigger 1 — `tr_article_tags_bump_updated_at_*`

### Problem

`articulos_cientificos.updated_at` is decorated with Hibernate's `@UpdateTimestamp`, which fires on direct entity updates. The article aggregate has a `@OneToMany(cascade = ALL, orphanRemoval = true)` collection of `ArticleTagEntity`. When feature 3's `Article.withAiAnalysis` returns a new aggregate with replaced tags, Hibernate emits:

1. `DELETE FROM articulo_tags WHERE articulo_id = ?` (orphan removal)
2. `INSERT INTO articulo_tags (...)` for each new tag
3. `UPDATE articulos_cientificos SET especialidad_id = ?` (only if it changed)

Step 3 is the only path that updates `articulos_cientificos`, and it only runs when the **column** changes. If the AI re-classifies an article to the **same** specialty and replaces tags, Hibernate emits steps 1 and 2 but no step 3 — `updated_at` stays put.

The matching query orders by `a.updated_at DESC`. So a re-analyzed article slides back in the list even though it just changed.

### Implementation

Three AFTER triggers (MySQL constraint: one event per trigger), each running a single UPDATE:

```sql
CREATE TRIGGER tr_article_tags_bump_updated_at_ins
AFTER INSERT ON articulo_tags
FOR EACH ROW
UPDATE articulos_cientificos
   SET updated_at = CURRENT_TIMESTAMP
 WHERE id = NEW.articulo_id;
```

The UPDATE/DELETE variants are identical except for the event and the reference (`NEW` vs `OLD`).

Single-statement triggers do not need a DELIMITER override — Flyway's MySQL parser handles them in the default `;`-terminated form.

## Trigger 2 — `tr_specialty_soft_delete_guard`

### Problem

`SpecialtyEntity` has `@SQLRestriction("activo = true")`, which hides soft-deleted rows from every JPQL read. The matching query joins `articulos_cientificos.especialidad_id` to a JPA entity that is filtered by this restriction. If a specialty is soft-deleted while articles still carry its FK, those articles silently drop out of the matching pipeline — even though the FK constraint is still satisfied at the SQL level. Worse, when feature 4 inherits the doctor's specialty onto a `paciente_contexto` row, soft-deleting that specialty later breaks the context's matching ability for the same reason.

The application layer does not catch this because `softDelete()` is a domain operation that only knows about the specialty aggregate.

### Implementation

A `BEFORE UPDATE` trigger with a multi-statement body — needs DELIMITER overrides so the inner `;` is not interpreted as a Flyway statement separator:

```sql
DELIMITER $$
CREATE TRIGGER tr_specialty_soft_delete_guard
BEFORE UPDATE ON especialidades
FOR EACH ROW
BEGIN
    IF OLD.activo = TRUE AND NEW.activo = FALSE THEN
        IF EXISTS (SELECT 1 FROM articulos_cientificos
                    WHERE especialidad_id = OLD.id LIMIT 1) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cannot soft-delete specialty: …';
        END IF;
        IF EXISTS (SELECT 1 FROM paciente_contexto
                    WHERE especialidad_id = OLD.id LIMIT 1) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cannot soft-delete specialty: …';
        END IF;
    END IF;
END$$
DELIMITER ;
```

`SIGNAL SQLSTATE '45000'` is MySQL's "user-defined exception" pattern. The thrown error propagates to JDBC as a `SQLException`, which Hibernate translates to a `org.hibernate.exception.GenericJDBCException`. The application sees a 500-level failure on the soft-delete call. That is the intended behaviour for now — surfacing this as a clean 409 requires catching the SQLState in `GlobalExceptionHandler`, which is deferred until the next snapshot.

### Decisions

- **Reject, not auto-NULL.** The trigger could `UPDATE` the dependent rows to `especialidad_id = NULL` instead of rejecting. Rejecting forces the operator to think — a silent NULL would orphan analytics in a way the operator never agreed to. If the operator wants to soft-delete, they re-tag the dependents first.
- **`LIMIT 1` on the `EXISTS` subquery.** Strictly redundant (`EXISTS` short-circuits), but explicit about intent and matches MySQL's documented optimisation pattern.
- **Order of checks.** Articles are checked before `paciente_contexto` because the article side typically has more rows and is more likely to surface a conflict — failing fast saves the second query.

## Procedure — `sp_match_articles_for_patient`

### Why a procedure

The matching query is the most complex piece of SQL in the codebase. Today it is one method on `ArticleRepositoryImpl`, written as JPQL. Feature 3 will add a third join condition. The procedure exists to:

1. Give a demoable, MySQL-Workbench-callable surface for the same query.
2. Hold the canonical SQL alongside the application's JPQL — `git diff` on the procedure tells future maintainers when the matching rules changed.
3. Provide a future migration path: if the JPQL version becomes a performance bottleneck, `ArticleRepositoryImpl` can be switched to `CALL sp_match_articles_for_patient(?, ?)` without restructuring callers.

The application **does not call the procedure today**. The two definitions of the matching query (Java JPQL + MySQL procedure) are kept in sync by convention. The spec snapshot for any future change to either side must update both.

### Signature

```sql
CREATE PROCEDURE sp_match_articles_for_patient(
    IN p_patient_id BINARY(16),
    IN p_limit      INT
)
```

- `BINARY(16)` matches the project's UUID storage decision (CLAUDE.md).
- `IN` parameters only — the procedure returns a result set, not OUT parameters. MySQL Workbench renders the result set in a tab; JDBC clients consume it through `CallableStatement.executeQuery()`.

### Body

```sql
SELECT DISTINCT a.*
  FROM articulos_cientificos a
  JOIN articulo_tags         t ON t.articulo_id = a.id
  JOIN paciente_contexto     c ON c.tipo  = t.tipo
                              AND c.valor = t.valor
 WHERE c.paciente_id = p_patient_id
 ORDER BY a.updated_at DESC
 LIMIT p_limit;
```

Mirrors the JPQL `ArticleRepositoryImpl.findMatchingArticlesForPaciente` runs. When feature 3 lands, both will gain `AND c.especialidad_id = a.especialidad_id` in the join.

## Tests

H2 does not see V13, so `@QuarkusTest` runs in the `test` profile cannot verify trigger/procedure behaviour. Two options for proving correctness:

1. **Manual smoke test** under the `mysql-local` profile (Docker MySQL on port 3307). Steps documented in `tasks.md`.
2. **Testcontainers MySQL** — not introduced in this snapshot to keep the change minimal. Adding it later is a separate spec.

For unit-level verification of the *SQL* (syntax / placeholder substitution), the migration file is exercised every time the `mysql-local` profile boots — a syntax error there would fail boot loudly.

## Cross-feature impact

| Existing feature   | Affected artifact                | Change                                                                                                                                                                                                  |
|--------------------|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `article`          | `articulo_tags` table             | Three AFTER triggers now fire on every write. Net effect: `articulos_cientificos.updated_at` is correctly bumped on cascaded tag changes — fixes the bug where matching ordering went stale post-AI-tag. |
| `specialty`        | `especialidades.activo` UPDATEs   | A guarded UPDATE: cannot flip TRUE→FALSE while dependents exist. The application's `SoftDeleteSpecialtyService` may now surface a 500 (until the SQLState mapper is added) if dependents are present.    |
| `article-ai-analysis` (planned) | inline JPQL matching query | Must stay in sync with `sp_match_articles_for_patient` when the specialty-join clause is added. Documented as a maintenance note in the procedure's header comment.                                      |
| `infrastructure` (Flyway config) | `application.properties`     | `quarkus.flyway.locations` now reads `db/migration,db/vendor_{vendor}`.                                                                                                                                  |

## Open technical decisions / risks

- **`SIGNAL` errors surface as 500 today.** The soft-delete guard throws a MySQL user-defined exception. Hibernate translates it to a generic 500 in `GlobalExceptionHandler.FallbackMapper`. A clean 409 mapping requires inspecting the SQLState in the handler. Flagged as follow-up work — not blocking because the operator's recourse is the same (re-tag dependents, retry).
- **H2 parity is lost.** Until we either add Java-backed H2 triggers or move tests to Testcontainers, dev (`./mvnw quarkus:dev`) and `@QuarkusTest` runs do not exercise these objects. Acceptable for a school project where the demo target is MySQL.
- **MySQL trigger storm on bulk operations.** A `DELETE … FROM articulo_tags WHERE articulo_id = ?` that removes 20 rows fires the trigger 20 times, all of which UPDATE the same parent row 20 times. Acceptable because: (a) the orphan-removal path is bounded by `cascade` semantics (at most one parent per cascade), (b) MySQL coalesces same-row UPDATEs in the binlog, (c) tag counts per article are typically <30.
