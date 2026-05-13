package itesm.medsync.infrastructure.vocabulary;

import itesm.medsync.domain.shared.model.TipoClinico;

import java.util.List;
import java.util.Map;

public record ParsedVocabularyFile(
        String slug,
        String version,
        Map<TipoClinico, List<String>> termsByType) {
}
