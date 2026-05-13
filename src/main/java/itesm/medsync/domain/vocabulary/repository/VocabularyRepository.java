package itesm.medsync.domain.vocabulary.repository;

import itesm.medsync.domain.vocabulary.model.Vocabulary;

import java.util.UUID;

/**
 * Lookup port for the per-specialty controlled vocabulary.
 *
 * Backed by an immutable in-memory map populated once at application startup
 * from JSON resources under {@code src/main/resources/vocabulary/}. Never
 * performs I/O after boot; all reads are O(1) and lock-free.
 *
 * This is a {@code *Repository} (not a {@code *Gateway}) because the vocabulary
 * files are owned by this codebase and shipped inside the application artifact.
 */
public interface VocabularyRepository {

    /**
     * Returns the loaded vocabulary for the specialty, or a {@link Vocabulary#empty}
     * placeholder if no file was loaded for it. Never returns {@code null}.
     */
    Vocabulary getVocabularyFor(UUID especialidadId);
}
