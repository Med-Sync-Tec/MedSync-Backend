package itesm.medsync.application.vocabulary;

import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.usecase.GetSpecialtyByIdUseCase;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.repository.VocabularyRepository;
import itesm.medsync.domain.vocabulary.usecase.GetVocabularyByEspecialidadUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class GetVocabularyByEspecialidadService implements GetVocabularyByEspecialidadUseCase {

    private final GetSpecialtyByIdUseCase getSpecialtyById;
    private final VocabularyRepository vocabularyRepository;

    @Inject
    public GetVocabularyByEspecialidadService(GetSpecialtyByIdUseCase getSpecialtyById,
                                              VocabularyRepository vocabularyRepository) {
        this.getSpecialtyById = getSpecialtyById;
        this.vocabularyRepository = vocabularyRepository;
    }

    @Override
    public Vocabulary execute(UUID especialidadId) {
        Specialty specialty = getSpecialtyById.execute(especialidadId);
        return vocabularyRepository.getVocabularyFor(specialty.getId());
    }
}
