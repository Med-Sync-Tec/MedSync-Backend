package itesm.medsync.application.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetMatchingArticlesByPatientServiceTest {

    @Mock
    ArticleRepository articleRepository;

    @Mock
    PatientRepository patientRepository;

    @InjectMocks
    GetMatchingArticlesByPatientService service;

    @Test
    @DisplayName("Happy path: delega al repository tras verificar paciente")
    void matchOk() {
        UUID pacienteId = UUID.randomUUID();
        Patient stub = new Patient(pacienteId, "EXP", "Ana", LocalDate.of(1990, Month.JANUARY, 1),
                "F", UUID.randomUUID(), true, null, null);
        when(patientRepository.findByUuid(pacienteId)).thenReturn(Optional.of(stub));
        Article article = Article.create("titulo", null, null, 2024, null,
                null, null, null, null, null);
        when(articleRepository.findMatchingArticlesForPaciente(eq(pacienteId), anyInt()))
                .thenReturn(List.of(article));

        List<Article> result = service.execute(pacienteId, 50);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Paciente inexistente: PatientNotFoundException, no consulta articles")
    void matchPatientNotFound() {
        UUID pacienteId = UUID.randomUUID();
        when(patientRepository.findByUuid(pacienteId)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> service.execute(pacienteId, 50));
        verify(articleRepository, never()).findMatchingArticlesForPaciente(any(), anyInt());
    }
}
