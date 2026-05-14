package itesm.medsync.interfaces.rest.consultaaianalysis;

import java.util.List;
import java.util.UUID;

/**
 * Result envelope for {@code POST /api/consultas/{consultaId}/analyze}.
 * Carries the consulta id (echoed), the doctor's specialty (used to scope
 * the vocabulary), {@code vocabularyStatus} for the
 * "no-vocabulary-loaded" disclaimer, and provenance metadata.
 */
public record ConsultaAnalysisResponse(
        String consultaId,
        UUID especialidadId,
        String especialidadSlug,
        String vocabularyStatus,
        List<SuggestionResponse> suggestions,
        String modelUsed,
        int promptTokens,
        int completionTokens) {
}
