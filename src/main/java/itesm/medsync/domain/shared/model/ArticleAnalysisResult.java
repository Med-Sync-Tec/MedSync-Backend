package itesm.medsync.domain.shared.model;

import java.util.List;
import java.util.UUID;

/**
 * Outcome of an article analysis round-trip.
 *
 * {@code modelUsed}, {@code promptTokens}, {@code completionTokens} are
 * surface-level observability captured from the upstream API's response
 * envelope and threaded back to the REST layer so the doctor sees usage
 * stats next to the result. {@code hadAbstract} echoes
 * {@link ArticleAnalysisRequest#hasAbstract()} so the frontend can render a
 * "fewer tags than usual — analyzed without abstract" disclaimer.
 */
public record ArticleAnalysisResult(
        UUID especialidadId,
        List<ExtractedTag> tags,
        String modelUsed,
        int promptTokens,
        int completionTokens,
        boolean hadAbstract) {
}
