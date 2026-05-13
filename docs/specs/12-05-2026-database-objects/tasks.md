# Feature: Database Objects — Implementation Tasks

Execute in order. Each item ≈ one commit. No Java code in this snapshot; everything is Flyway + documentation.

## Flyway layout

- [x] Update `quarkus.flyway.locations` in `src/main/resources/application.properties` to `db/migration,db/vendor_{vendor}` (sibling, not sub-path — Flyway discards overlapping sub-locations).
- [x] Create `src/main/resources/db/vendor_mysql/` and `src/main/resources/db/vendor_h2/`. The H2 folder is empty for now.

## Migration

- [x] Add `src/main/resources/db/vendor_mysql/V13__add_database_objects.sql` with three logical objects (5 MySQL statements, since one logical trigger is split into 3 per-event triggers):
  - `tr_article_tags_bump_updated_at_ins / _upd / _del` — bumps `articulos_cientificos.updated_at`.
  - `tr_specialty_soft_delete_guard` — rejects soft-deletes while dependents exist.
  - `sp_match_articles_for_patient(BINARY(16), INT)` — encapsulates the patient-matching query.

## Verification (operator-run)

- [x] `./mvnw test` — full suite green on H2 (proves the migration is skipped on H2 and existing 400 tests are unaffected).
- [ ] Boot `./mvnw quarkus:dev -Dquarkus.profile=mysql-local` and confirm V13 is applied. Flyway log line should read `Migrating schema "medsync" to version "13 - add database objects"`.
- [ ] Connect via MySQL Workbench (or the MySQL CLI) and run:
  ```sql
  SHOW TRIGGERS FROM medsync;
  -- Expect 4 rows: tr_article_tags_bump_updated_at_ins / _upd / _del
  --                tr_specialty_soft_delete_guard
  SHOW PROCEDURE STATUS WHERE Db = 'medsync';
  -- Expect 1 row: sp_match_articles_for_patient
  ```

## Manual smoke tests (operator, against `mysql-local`)

- [ ] **Trigger 1 — happy path**. Insert a row into `articulo_tags` for an existing article. Confirm `SELECT updated_at FROM articulos_cientificos WHERE id = …` is `CURRENT_TIMESTAMP`.
- [ ] **Trigger 1 — delete path**. Delete the tag. Confirm `updated_at` moved again.
- [ ] **Trigger 2 — happy path**. Pick a specialty with no dependents (none of the seeded ones unless you've tagged articles). `UPDATE especialidades SET activo = FALSE WHERE id = ?;` succeeds.
- [ ] **Trigger 2 — rejection path**. Manually tag an article with a specialty's id (`UPDATE articulos_cientificos SET especialidad_id = ? WHERE id = ?`), then attempt the soft-delete. Confirm MySQL returns:
  ```
  ERROR 1644 (45000): Cannot soft-delete specialty: one or more articles still reference it.
  ```
- [ ] **Procedure — happy path**. Pick a patient with at least one `paciente_contexto` row whose `(tipo, valor)` matches a tag on some article. Run:
  ```sql
  CALL sp_match_articles_for_patient(UUID_TO_BIN(?), 10);
  ```
  Confirm the result set matches what `GET /api/patients/{id}/matching-articles` returns (modulo the limit).
- [ ] **Procedure — no matches**. Pick a patient with no contexts. Confirm the procedure returns zero rows (no error).

## Documentation

- [x] Create `docs/specs/12-05-2026-database-objects/{requirements,design,tasks}.md` (this folder).
- [x] Cross-reference the migration from the affected feature snapshots: add a "Database-side enforcement" subsection to `12-05-2026-specialty/design.md` (soft-delete guard) and `12-05-2026-article-ai-analysis/design.md` (updated_at trigger + parallel procedure).
- [x] Update `docs/conventions/migrations.md` documenting the vendor-path Flyway pattern.
- [x] Add `database-objects` to `docs/specs/README.md`'s feature index.

## Out of scope (deferred to a follow-up snapshot)

- The remaining 5 objects needed to fully satisfy the course rubric (1 procedure, 2 functions, 3 triggers).
- Mapping `SIGNAL SQLSTATE '45000'` to a clean HTTP 409 in `GlobalExceptionHandler`.
- Java-backed H2 equivalents (or Testcontainers MySQL) so the triggers fire in `@QuarkusTest` runs.

## Wrap-up

- [ ] **Do not** write `summary.md` in this session — filled post-implementation in a future session.
