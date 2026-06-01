package itesm.medsync.domain.article.model;

/**
 * Summary of a completed auto-specialty analysis pass.
 *
 * @param attempted    total articles that had {@code especialidad_id = NULL} at query time
 * @param succeeded    articles for which the gateway returned a valid result and the article was saved
 * @param skippedBlank articles skipped because all three text fields (titulo, abstractText, keywords) were blank
 * @param failed       articles for which the gateway threw any exception
 */
public record AutoAnalysisSummary(int attempted, int succeeded, int skippedBlank, int failed) {

    @Override
    public String toString() {
        return String.format(
                "attempted=%d succeeded=%d skippedBlank=%d failed=%d",
                attempted, succeeded, skippedBlank, failed);
    }
}
