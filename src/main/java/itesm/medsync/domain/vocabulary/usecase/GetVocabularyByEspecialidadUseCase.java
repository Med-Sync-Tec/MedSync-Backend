package itesm.medsync.domain.vocabulary.usecase;

import itesm.medsync.domain.vocabulary.model.Vocabulary;

import java.util.UUID;

public interface GetVocabularyByEspecialidadUseCase {

    Vocabulary execute(UUID especialidadId);
}
