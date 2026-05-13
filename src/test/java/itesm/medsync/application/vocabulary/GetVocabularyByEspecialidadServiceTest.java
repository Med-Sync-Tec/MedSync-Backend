package itesm.medsync.application.vocabulary;

import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.specialty.exception.SpecialtyNotFoundException;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.usecase.GetSpecialtyByIdUseCase;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.model.VocabularyTerm;
import itesm.medsync.domain.vocabulary.repository.VocabularyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetVocabularyByEspecialidadServiceTest {

    @Mock
    GetSpecialtyByIdUseCase getSpecialtyById;

    @Mock
    VocabularyRepository vocabularyRepository;

    @InjectMocks
    GetVocabularyByEspecialidadService service;

    @Test
    @DisplayName("Especialidad desconocida → SpecialtyNotFoundException")
    void unknownSpecialty() {
        UUID id = UUID.randomUUID();
        when(getSpecialtyById.execute(id)).thenThrow(new SpecialtyNotFoundException(id));

        assertThrows(SpecialtyNotFoundException.class, () -> service.execute(id));
    }

    @Test
    @DisplayName("Especialidad existente con vocabulary cargado → lo devuelve")
    void loadedVocabulary() {
        UUID id = UUID.randomUUID();
        Specialty specialty = new Specialty(id, "Cardiología", "cardiologia", null, true, null, null);
        Vocabulary loaded = new Vocabulary(id, "cardiologia", "2026-05-12",
                Map.of(TipoClinico.ENFERMEDAD,
                        List.of(new VocabularyTerm(TipoClinico.ENFERMEDAD, "Hipertensión"))));

        when(getSpecialtyById.execute(id)).thenReturn(specialty);
        when(vocabularyRepository.getVocabularyFor(id)).thenReturn(loaded);

        Vocabulary result = service.execute(id);

        assertSame(loaded, result);
    }

    @Test
    @DisplayName("Especialidad existente sin vocabulary → devuelve Vocabulary.empty(...)")
    void noVocabularyLoaded() {
        UUID id = UUID.randomUUID();
        Specialty specialty = new Specialty(id, "Pediatría", "pediatria", null, true, null, null);
        Vocabulary empty = Vocabulary.empty(id, "pediatria");

        when(getSpecialtyById.execute(id)).thenReturn(specialty);
        when(vocabularyRepository.getVocabularyFor(id)).thenReturn(empty);

        Vocabulary result = service.execute(id);

        assertEquals("empty", result.getVersion());
        assertEquals(0, result.totalTerms());
    }
}
