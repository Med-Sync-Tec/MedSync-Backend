package itesm.medsync.domain.vocabulary.repository;

import itesm.medsync.domain.vocabulary.model.Vocabulary;

import java.util.UUID;

public interface VocabularyRepository {

    Vocabulary getVocabularyFor(UUID especialidadId);
}
