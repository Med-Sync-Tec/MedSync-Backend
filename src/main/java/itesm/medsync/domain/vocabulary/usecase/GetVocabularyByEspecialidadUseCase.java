package itesm.medsync.domain.vocabulary.usecase;

import itesm.medsync.domain.vocabulary.model.Vocabulary;

import java.util.UUID;

/**
 * Returns the vocabulary loaded for a specialty (debug / COO operation).
 *
 * Throws {@code SpecialtyNotFoundException} if the id does not resolve to an
 * existing specialty. Returns {@link Vocabulary#empty} if the specialty exists
 * but has no JSON file shipped — AI flows handle that case gracefully.
 *
 * AI features 3 and 4 inject the underlying {@code VocabularyRepository}
 * directly rather than going through this use case, because their flow is
 * "resolve specialty → load vocabulary → call LLM → save tags" and a
 * one-line "look up the vocabulary" passthrough would add no behavior.
 */
public interface GetVocabularyByEspecialidadUseCase {

    Vocabulary execute(UUID especialidadId);
}
