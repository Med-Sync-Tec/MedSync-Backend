package itesm.medsync.interfaces.rest.vocabulary;

import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.model.VocabularyTerm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates a {@link Vocabulary} aggregate to the outbound {@link VocabularyResponse}.
 *
 * Sorts each bucket alphabetically (case-insensitive) so the JSON payload is stable
 * for human inspection and idempotent for any consumer that diffs responses.
 */
public final class VocabularyRestMapper {

    private VocabularyRestMapper() {
    }

    public static VocabularyResponse toResponse(Vocabulary v) {
        Map<String, List<String>> termsByType = new LinkedHashMap<>();
        for (TipoClinico tipo : TipoClinico.values()) {
            List<String> sorted = v.getTermsFor(tipo).stream()
                    .map(VocabularyTerm::getValor)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            termsByType.put(tipo.name(), sorted);
        }
        return new VocabularyResponse(
                v.getEspecialidadId(),
                v.getEspecialidadSlug(),
                v.getVersion(),
                termsByType,
                v.totalTerms());
    }
}
