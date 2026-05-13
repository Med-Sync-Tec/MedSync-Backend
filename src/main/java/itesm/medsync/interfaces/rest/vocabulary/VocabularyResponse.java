package itesm.medsync.interfaces.rest.vocabulary;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record VocabularyResponse(
        UUID especialidadId,
        String especialidadSlug,
        String version,
        Map<String, List<String>> termsByType,
        int totalTerms) {
}
