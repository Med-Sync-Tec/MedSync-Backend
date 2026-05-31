package itesm.medsync.domain.article.usecase;

import itesm.medsync.domain.article.model.AutoAnalysisSummary;

/**
 * Use case: classify every article that has no specialty assigned yet.
 *
 * <p>Each article is analyzed in its own transaction; gateway failures are
 * logged and skipped without aborting the batch. The pass is idempotent —
 * only articles with {@code especialidad_id = NULL} are attempted.
 */
public interface AutoAnalyzeNewArticlesUseCase {

    /**
     * Runs the auto-specialty analysis pass.
     *
     * @return a summary of how many articles were attempted, succeeded, skipped
     *         (all text blank), or failed (gateway error).
     */
    AutoAnalysisSummary execute();
}
