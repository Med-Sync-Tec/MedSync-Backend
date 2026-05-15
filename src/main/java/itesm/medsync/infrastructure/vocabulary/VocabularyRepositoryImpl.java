package itesm.medsync.infrastructure.vocabulary;

import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.repository.VocabularyRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;

/**
 * In-memory adapter for {@link VocabularyRepository}.
 *
 * The backing map is populated once by {@link VocabularyLoader#onStart} during
 * application boot, then never mutated. A {@code volatile} reference is used so
 * the visibility of the install is correct without paying for synchronization
 * on the (very hot) read path.
 *
 * Before {@code installMap} is called, the field holds {@code Map.of()} so any
 * read that races startup gets an empty vocabulary instead of a NullPointerException.
 */
@ApplicationScoped
public class VocabularyRepositoryImpl implements VocabularyRepository {

    private volatile Map<UUID, Vocabulary> bySpecialtyId = Map.of();

    @Override
    public Vocabulary getVocabularyFor(UUID especialidadId) {
        Vocabulary v = bySpecialtyId.get(especialidadId);
        if (v != null) {
            return v;
        }
        return Vocabulary.empty(especialidadId, "unknown");
    }

    /**
     * One-shot installer called by {@link VocabularyLoader} at startup. Package-private
     * by design — no other class should call it. Wraps the input with {@code Map.copyOf}
     * to guarantee the stored reference is unmodifiable.
     */
    void installMap(Map<UUID, Vocabulary> map) {
        this.bySpecialtyId = Map.copyOf(map);
    }
}
