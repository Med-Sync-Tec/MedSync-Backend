package itesm.medsync.infrastructure.vocabulary;

import itesm.medsync.domain.shared.model.TipoClinico;

import java.util.List;
import java.util.Map;

/**
 * Intermediate result of parsing a vocabulary JSON file — raw strings only,
 * not yet promoted to {@link itesm.medsync.domain.vocabulary.model.VocabularyTerm} instances.
 *
 * The loader uses this shape so the parser can be tested in isolation from the
 * domain constructor, and so a future schema-v2 reader could produce the same
 * intermediate without leaking new fields into the domain model.
 */
public record ParsedVocabularyFile(
        String slug,
        String version,
        Map<TipoClinico, List<String>> termsByType) {
}
