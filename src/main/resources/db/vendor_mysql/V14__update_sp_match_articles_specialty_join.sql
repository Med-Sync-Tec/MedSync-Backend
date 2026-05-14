-- =============================================================================
-- V14 — Update sp_match_articles_for_patient with the specialty join clause
-- =============================================================================
-- Feature 3 (article-ai-analysis) tightens the patient-matching query so it
-- scopes matches to articles whose especialidad_id equals the context's
-- especialidad_id. The Java side (ArticleRepositoryImpl) is updated in code;
-- this migration keeps the MySQL-side procedure in sync so direct DB calls
-- (Workbench demos, analytics jobs) follow the same rule.
--
-- SQL NULL ≠ NULL is intentional: un-analyzed articles (NULL especialidad_id)
-- and legacy contexts (NULL especialidad_id) are excluded until enriched.
-- =============================================================================

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
      JOIN paciente_contexto     c ON c.tipo            = t.tipo
                                  AND c.valor           = t.valor
                                  AND c.especialidad_id = a.especialidad_id
     WHERE c.paciente_id = p_patient_id
     ORDER BY a.updated_at DESC
     LIMIT p_limit;
END$$

DELIMITER ;
