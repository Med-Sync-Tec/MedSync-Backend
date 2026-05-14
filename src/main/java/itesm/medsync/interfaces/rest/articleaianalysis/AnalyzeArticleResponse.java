package itesm.medsync.interfaces.rest.articleaianalysis;

import java.util.List;
import java.util.UUID;

/**
 * Result envelope for {@code POST /api/articles/{id}/analyze}.
 *
 * Carries the persisted state ({@code articleId}, {@code especialidadId},
 * {@code especialidadNombre}, {@code tags}) plus provenance metadata so the
 * doctor sees usage stats ("Analyzed with llama-3.3-70b-versatile in 1.2k
 * tokens") and an abstract-availability disclaimer.
 */
public record AnalyzeArticleResponse(
        UUID articleId,
        UUID especialidadId,
        String especialidadNombre,
        List<AnalyzedTagResponse> tags,
        String modelUsed,
        int promptTokens,
        int completionTokens,
        boolean hadAbstract) {
}
