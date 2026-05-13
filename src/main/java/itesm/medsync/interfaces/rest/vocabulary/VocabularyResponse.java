package itesm.medsync.interfaces.rest.vocabulary;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Outbound DTO for the debug endpoint.
 *
 * {@code termsByType} always contains the four {@link itesm.medsync.domain.shared.model.TipoClinico}
 * keys (as {@code String}), values flattened to alphabetically sorted lists for stable
 * consumer parsing. Missing buckets serialize as empty arrays.
 */
public record VocabularyResponse(
        UUID especialidadId,
        String especialidadSlug,
        String version,
        Map<String, List<String>> termsByType,
        int totalTerms) {
}
