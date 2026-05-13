package itesm.medsync.infrastructure.vocabulary;

import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.repository.VocabularyRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;

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

    void installMap(Map<UUID, Vocabulary> map) {
        this.bySpecialtyId = Map.copyOf(map);
    }
}
