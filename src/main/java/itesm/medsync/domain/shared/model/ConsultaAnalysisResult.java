package itesm.medsync.domain.shared.model;

import java.util.List;

/**
 * Outcome of a SOAP-note analysis — feature 4. No {@code hadAbstract} here
 * because a consulta always has text (the 400 fires earlier if it does not).
 */
public record ConsultaAnalysisResult(
        List<ExtractedTag> tags,
        String modelUsed,
        int promptTokens,
        int completionTokens) {
}
