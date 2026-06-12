package itesm.medsync.application.consultaaianalysis;

import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisException;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisTimeoutException;
import itesm.medsync.domain.consultaaianalysis.exception.InvalidConsultaDataException;
import itesm.medsync.domain.consultaaianalysis.exception.UserHasNoSpecialtyException;
import itesm.medsync.domain.consultaaianalysis.model.ConsultaAnalysis;
import itesm.medsync.domain.consultaaianalysis.model.VocabularyStatus;
import itesm.medsync.domain.hospital.exception.ConsultaNotFoundException;
import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.repository.HospitalGateway;
import itesm.medsync.domain.shared.model.ConsultaAnalysisRequest;
import itesm.medsync.domain.shared.model.ConsultaAnalysisResult;
import itesm.medsync.domain.shared.model.ExtractedTag;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.shared.repository.AiAnalysisGateway;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.model.VocabularyTerm;
import itesm.medsync.domain.vocabulary.repository.VocabularyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyzeConsultaWithAiServiceTest {

    @Mock
    HospitalGateway hospitalGateway;

    @Mock
    VocabularyRepository vocabularyRepository;

    @Mock
    AiAnalysisGateway aiGateway;

    @InjectMocks
    AnalyzeConsultaWithAiService service;

    private static final UUID SPECIALTY_ID = UUID.randomUUID();
    private static final String CONSULTA_ID = "c-1";

    private User caller(UUID especialidadId) {
        return new User(UUID.randomUUID(), "Dra. Ana", "ana@tec.mx",
                especialidadId, UUID.randomUUID(), true, null);
    }

    private Consulta richConsulta() {
        return new Consulta(
                CONSULTA_ID, "exp-1", LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0),
                "Dolor torácico", "Refiere disnea", "TA 150/95",
                "HTA estadio 2", "Iniciar Losartán", "Losartán 50mg",
                "I10 Hipertensión", null, null);
    }

    private Consulta blankConsulta() {
        return new Consulta(
                CONSULTA_ID, "exp-1", LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0),
                null, null, null, null, null, null, null, null, null);
    }

    private Vocabulary cardioVocab() {
        return new Vocabulary(SPECIALTY_ID, "cardiologia", "v1",
                Map.of(
                        TipoClinico.ENFERMEDAD,
                        List.of(new VocabularyTerm(TipoClinico.ENFERMEDAD, "Hipertensión arterial")),
                        TipoClinico.MEDICAMENTO,
                        List.of(new VocabularyTerm(TipoClinico.MEDICAMENTO, "Losartán"))));
    }

    @Test
    @DisplayName("Happy path: vocab poblada, gateway devuelve 3 tags → POPULATED con 3 sugerencias")
    void happyPath() {
        when(hospitalGateway.findConsultaById(CONSULTA_ID)).thenReturn(Optional.of(richConsulta()));
        when(vocabularyRepository.getVocabularyFor(SPECIALTY_ID)).thenReturn(cardioVocab());

        ConsultaAnalysisResult gatewayResult = new ConsultaAnalysisResult(
                List.of(
                        new ExtractedTag(TipoClinico.ENFERMEDAD, "Hipertensión arterial"),
                        new ExtractedTag(TipoClinico.MEDICAMENTO, "Losartán"),
                        new ExtractedTag(TipoClinico.ENFERMEDAD, "Hipertensión arterial")),
                "llama-3.3-70b-versatile", 800, 120);
        when(aiGateway.analyzeConsultaText(any(ConsultaAnalysisRequest.class)))
                .thenReturn(gatewayResult);

        ConsultaAnalysis result = service.execute(CONSULTA_ID, caller(SPECIALTY_ID));

        assertEquals(VocabularyStatus.POPULATED, result.vocabularyStatus());
        assertEquals(3, result.suggestions().size());
        assertEquals(800, result.promptTokens());
        assertEquals("llama-3.3-70b-versatile", result.modelUsed());
        assertEquals(SPECIALTY_ID, result.especialidadId());
        assertEquals("cardiologia", result.especialidadSlug());
    }

    @Test
    @DisplayName("Caller sin especialidadId → UserHasNoSpecialtyException; gateway no llamado")
    void noSpecialty() {
        assertThrows(UserHasNoSpecialtyException.class,
                () -> service.execute(CONSULTA_ID, caller(null)));
        verifyNoInteractions(hospitalGateway, vocabularyRepository, aiGateway);
    }

    @Test
    @DisplayName("Consulta no existe → ConsultaNotFoundException; gateway no llamado")
    void consultaNotFound() {
        when(hospitalGateway.findConsultaById(CONSULTA_ID)).thenReturn(Optional.empty());

        assertThrows(ConsultaNotFoundException.class,
                () -> service.execute(CONSULTA_ID, caller(SPECIALTY_ID)));
        verifyNoInteractions(vocabularyRepository, aiGateway);
    }

    @Test
    @DisplayName("Consulta con todos los SOAP blank → InvalidConsultaDataException; gateway no llamado")
    void allBlankRejected() {
        when(hospitalGateway.findConsultaById(CONSULTA_ID)).thenReturn(Optional.of(blankConsulta()));

        assertThrows(InvalidConsultaDataException.class,
                () -> service.execute(CONSULTA_ID, caller(SPECIALTY_ID)));
        verifyNoInteractions(aiGateway);
    }

    @Test
    @DisplayName("Vocab vacía → status EMPTY; gateway NO llamado")
    void emptyVocabShortCircuit() {
        when(hospitalGateway.findConsultaById(CONSULTA_ID)).thenReturn(Optional.of(richConsulta()));
        when(vocabularyRepository.getVocabularyFor(SPECIALTY_ID))
                .thenReturn(Vocabulary.empty(SPECIALTY_ID, "cardiologia"));

        ConsultaAnalysis result = service.execute(CONSULTA_ID, caller(SPECIALTY_ID));

        assertEquals(VocabularyStatus.EMPTY, result.vocabularyStatus());
        assertTrue(result.suggestions().isEmpty());
        assertEquals(0, result.promptTokens());
        assertEquals(0, result.completionTokens());
        verifyNoInteractions(aiGateway);
    }

    @Test
    @DisplayName("Gateway lanza AiAnalysisException → se propaga")
    void gatewayExceptionPropagates() {
        when(hospitalGateway.findConsultaById(CONSULTA_ID)).thenReturn(Optional.of(richConsulta()));
        when(vocabularyRepository.getVocabularyFor(SPECIALTY_ID)).thenReturn(cardioVocab());
        when(aiGateway.analyzeConsultaText(any())).thenThrow(new AiAnalysisException("boom"));

        assertThrows(AiAnalysisException.class,
                () -> service.execute(CONSULTA_ID, caller(SPECIALTY_ID)));
    }

    @Test
    @DisplayName("Gateway lanza AiAnalysisTimeoutException → se propaga")
    void gatewayTimeoutPropagates() {
        when(hospitalGateway.findConsultaById(CONSULTA_ID)).thenReturn(Optional.of(richConsulta()));
        when(vocabularyRepository.getVocabularyFor(SPECIALTY_ID)).thenReturn(cardioVocab());
        when(aiGateway.analyzeConsultaText(any())).thenThrow(new AiAnalysisTimeoutException("slow"));

        assertThrows(AiAnalysisTimeoutException.class,
                () -> service.execute(CONSULTA_ID, caller(SPECIALTY_ID)));
    }

    @Test
    @DisplayName("Texto SOAP truncado se reenvía al gateway sin más modificaciones desde el joiner")
    void truncatedSoapForwarded() {
        String huge = "x".repeat(60_000);
        Consulta huguelta = new Consulta(
                CONSULTA_ID, "exp-1", LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0),
                huge, null, null, null, null, null, null, null, null);
        when(hospitalGateway.findConsultaById(CONSULTA_ID)).thenReturn(Optional.of(huguelta));
        when(vocabularyRepository.getVocabularyFor(SPECIALTY_ID)).thenReturn(cardioVocab());

        ConsultaAnalysisResult gatewayResult = new ConsultaAnalysisResult(
                List.of(new ExtractedTag(TipoClinico.ENFERMEDAD, "Hipertensión arterial")),
                "llama-3.3-70b-versatile", 100, 20);
        ArgumentCaptor<ConsultaAnalysisRequest> captor = ArgumentCaptor.forClass(ConsultaAnalysisRequest.class);
        when(aiGateway.analyzeConsultaText(captor.capture())).thenReturn(gatewayResult);

        service.execute(CONSULTA_ID, caller(SPECIALTY_ID));

        assertEquals(50_000, captor.getValue().consultaText().length());
    }
}
