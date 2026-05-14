-- =============================================================================
-- V13 — Database-side objects (MySQL only)
-- =============================================================================
-- This migration adds three MySQL-side constructs that complement application
-- logic and fix two latent bugs:
--
--   1. tr_article_tags_bump_updated_at_{ins,upd,del}
--      Three AFTER triggers on articulo_tags that bump
--      articulos_cientificos.updated_at whenever the article's tag list
--      changes. Hibernate's @UpdateTimestamp only fires on direct entity
--      updates, not on cascaded child-table writes — so without this trigger,
--      AI tag replacement (feature 3) does NOT bump updated_at and the
--      matching query's ORDER BY a.updated_at DESC silently misorders results.
--      The three triggers are MySQL's per-event constraint (one event per
--      trigger); together they form one logical guarantee.
--
--   2. tr_specialty_soft_delete_guard
--      BEFORE UPDATE on especialidades. Rejects a soft-delete (activo
--      flipping TRUE → FALSE) while articles or paciente_contexto rows still
--      reference the specialty. Without it, soft-deleting a specialty leaves
--      dangling references that pass the FK constraint but break the
--      matching query (because @SQLRestriction hides the parent row).
--
--   3. sp_match_articles_for_patient
--      Server-side encapsulation of the patient-matching query. Today the
--      application runs an inline JPQL query for the same purpose; the
--      procedure exists for direct DB calls (MySQL Workbench during demos,
--      future analytics jobs, etc.) and for one-shot performance tuning
--      without changing the Java side.
--
-- Vendor-specific because: H2 in MODE=MySQL does not support MySQL's full
-- procedural syntax (DELIMITER, SIGNAL, multi-statement triggers with
-- BEGIN…END blocks). This file lives in db/migration/mysql so dev (H2) skips
-- it and prod / mysql-local apply it.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- TRIGGERS — bump articulos_cientificos.updated_at on tag changes
-- -----------------------------------------------------------------------------
-- Single-statement triggers don't need a DELIMITER override.

DROP TRIGGER IF EXISTS tr_article_tags_bump_updated_at_ins;
CREATE TRIGGER tr_article_tags_bump_updated_at_ins
AFTER INSERT ON articulo_tags
FOR EACH ROW
UPDATE articulos_cientificos
   SET updated_at = CURRENT_TIMESTAMP
 WHERE id = NEW.articulo_id;

DROP TRIGGER IF EXISTS tr_article_tags_bump_updated_at_upd;
CREATE TRIGGER tr_article_tags_bump_updated_at_upd
AFTER UPDATE ON articulo_tags
FOR EACH ROW
UPDATE articulos_cientificos
   SET updated_at = CURRENT_TIMESTAMP
 WHERE id = NEW.articulo_id;

DROP TRIGGER IF EXISTS tr_article_tags_bump_updated_at_del;
CREATE TRIGGER tr_article_tags_bump_updated_at_del
AFTER DELETE ON articulo_tags
FOR EACH ROW
UPDATE articulos_cientificos
   SET updated_at = CURRENT_TIMESTAMP
 WHERE id = OLD.articulo_id;


-- -----------------------------------------------------------------------------
-- TRIGGER — block soft-delete of a specialty still in use
-- -----------------------------------------------------------------------------
-- Multi-statement trigger body needs a DELIMITER override so the inner ';'
-- is not interpreted as a statement terminator by the SQL parser.

DROP TRIGGER IF EXISTS tr_specialty_soft_delete_guard;

DELIMITER $$

CREATE TRIGGER tr_specialty_soft_delete_guard
BEFORE UPDATE ON especialidades
FOR EACH ROW
BEGIN
    -- We only act when the row is transitioning from active to inactive.
    -- Other UPDATEs (renames, descriptions, hard-deletes of inactive rows)
    -- are unaffected.
    IF OLD.activo = TRUE AND NEW.activo = FALSE THEN
        IF EXISTS (
            SELECT 1 FROM articulos_cientificos
             WHERE especialidad_id = OLD.id
             LIMIT 1
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cannot soft-delete specialty: one or more articles still reference it. Re-tag those articles first.';
        END IF;

        IF EXISTS (
            SELECT 1 FROM paciente_contexto
             WHERE especialidad_id = OLD.id
             LIMIT 1
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Cannot soft-delete specialty: one or more paciente_contexto entries still reference it.';
        END IF;
    END IF;
END$$

DELIMITER ;


-- -----------------------------------------------------------------------------
-- PROCEDURE — encapsulated patient-matching query
-- -----------------------------------------------------------------------------
-- Returns the articles whose tags overlap a patient's clinical context.
-- The query mirrors the inline JPQL used by ArticleRepositoryImpl today;
-- when feature 3 (article-ai-analysis) ships, both sides will be updated to
-- add `AND c.especialidad_id = a.especialidad_id` in the join.
--
-- Parameters:
--   p_patient_id  BINARY(16) — the patient whose context drives the match
--   p_limit       INT        — max rows to return (caller sets pagination)

DROP PROCEDURE IF EXISTS sp_match_articles_for_patient;

DELIMITER $$

CREATE PROCEDURE sp_match_articles_for_patient(
    IN p_patient_id BINARY(16),
    IN p_limit      INT
)
BEGIN
    SELECT DISTINCT a.*
      FROM articulos_cientificos a
      JOIN articulo_tags         t ON t.articulo_id = a.id
      JOIN paciente_contexto     c ON c.tipo  = t.tipo
                                  AND c.valor = t.valor
     WHERE c.paciente_id = p_patient_id
     ORDER BY a.updated_at DESC
     LIMIT p_limit;
END$$

DELIMITER ;
