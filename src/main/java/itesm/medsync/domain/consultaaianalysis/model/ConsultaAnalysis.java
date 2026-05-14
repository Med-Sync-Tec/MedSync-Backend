package itesm.medsync.domain.consultaaianalysis.model;

import itesm.medsync.domain.shared.model.ExtractedTag;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Outcome of a consulta-analysis call — not persisted; the frontend renders
 * it for the doctor to accept selectively. Bundles provenance metadata
 * (model, token usage) so the UI can show usage stats next to the
 * suggestions.
 *
 * Invariant: {@link VocabularyStatus#EMPTY} ⇒ no LLM call was made, so
 * {@code suggestions} must be empty and both token counts must be zero.
 * The compact constructor enforces this so future callers cannot pair
 * the EMPTY status with stale data from a previous run.
 */
public record ConsultaAnalysis(
        String consultaId,
        UUID especialidadId,
        String especialidadSlug,
        VocabularyStatus vocabularyStatus,
        List<ExtractedTag> suggestions,
        String modelUsed,
        int promptTokens,
        int completionTokens) {

    public ConsultaAnalysis {
        Objects.requireNonNull(consultaId, "consultaId");
        Objects.requireNonNull(especialidadId, "especialidadId");
        Objects.requireNonNull(vocabularyStatus, "vocabularyStatus");
        Objects.requireNonNull(suggestions, "suggestions");
        if (vocabularyStatus == VocabularyStatus.EMPTY) {
            if (!suggestions.isEmpty()) {
                throw new IllegalArgumentException(
                        "EMPTY vocabulary status cannot carry suggestions — the LLM was not called");
            }
            if (promptTokens != 0 || completionTokens != 0) {
                throw new IllegalArgumentException(
                        "EMPTY vocabulary status cannot carry non-zero token counts — the LLM was not called");
            }
        }
        suggestions = List.copyOf(suggestions);
    }
}
