package itesm.medsync.interfaces.rest.consultaaianalysis;

import itesm.medsync.domain.consultaaianalysis.model.ConsultaAnalysis;
import itesm.medsync.domain.shared.model.ExtractedTag;

import java.util.List;

/**
 * Maps {@link ConsultaAnalysis} (domain) into the REST response. Static —
 * no state, no DI. Lowercases tipo in suggestion responses to match the
 * convention used by the existing patient-context single-entry POST, so
 * the frontend can pipe suggestions straight back into bulk-add without
 * case transforms.
 */
public final class ConsultaAnalysisRestMapper {

    private ConsultaAnalysisRestMapper() {
    }

    public static ConsultaAnalysisResponse toResponse(ConsultaAnalysis analysis) {
        List<SuggestionResponse> suggestions = analysis.suggestions().stream()
                .map(ConsultaAnalysisRestMapper::toSuggestion)
                .toList();
        return new ConsultaAnalysisResponse(
                analysis.consultaId(),
                analysis.especialidadId(),
                analysis.especialidadSlug(),
                analysis.vocabularyStatus().name(),
                suggestions,
                analysis.modelUsed(),
                analysis.promptTokens(),
                analysis.completionTokens());
    }

    private static SuggestionResponse toSuggestion(ExtractedTag tag) {
        return new SuggestionResponse(tag.tipo().name().toLowerCase(), tag.valor());
    }
}
