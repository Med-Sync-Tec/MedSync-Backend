package itesm.medsync.application.pubmedautospecialty;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.model.AutoAnalysisSummary;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisException;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisTimeoutException;
import itesm.medsync.domain.shared.model.ArticleAnalysisRequest;
import itesm.medsync.domain.shared.model.ArticleAnalysisResult;
import itesm.medsync.domain.shared.model.ExtractedTag;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.shared.repository.AiAnalysisGateway;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.model.VocabularyTerm;
import itesm.medsync.domain.vocabulary.repository.VocabularyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AutoAnalyzeNewArticlesService}.
 *
 * <p>The service is constructed manually (not via {@code @InjectMocks}) so we
 * can inject the {@code enabled} flag through the constructor.
 */
@ExtendWith(MockitoExtension.class)
class AutoAnalyzeNewArticlesServiceTest {

    @Mock
    ArticleRepository articleRepository;

    @Mock
    SpecialtyRepository specialtyRepository;

    @Mock
    VocabularyRepository vocabularyRepository;

    @Mock
    AiAnalysisGateway aiGateway;

    @Mock
    ArticleSpecialtySaver saver;

    // ------------------------------------------------------------------ helpers

    private AutoAnalyzeNewArticlesService serviceEnabled() {
        return new AutoAnalyzeNewArticlesService(
                articleRepository, specialtyRepository, vocabularyRepository,
                aiGateway, saver, true, 10, 0L);
    }

    private AutoAnalyzeNewArticlesService serviceDisabled() {
        return new AutoAnalyzeNewArticlesService(
                articleRepository, specialtyRepository, vocabularyRepository,
                aiGateway, saver, false, 10, 0L);
    }

    private Article articleWithText(String titulo, String abs, String keywords) {
        return Article.create(
                titulo == null || titulo.isBlank() ? "Título genérico" : titulo,
                "Autores", "Revista", 2024, "May",
                null, abs, keywords, "Journal Article",
                "https://pubmed.example/" + UUID.randomUUID());
    }

    private Specialty cardiologia(UUID id) {
        return new Specialty(id, "Cardiología", "cardiologia", "Especialidad del corazón",
                true, null, null);
    }

    private Vocabulary cardioVocab(UUID specialtyId) {
        return new Vocabulary(specialtyId, "cardiologia", "v1",
                Map.of(
                        TipoClinico.ENFERMEDAD,
                        List.of(new VocabularyTerm(TipoClinico.ENFERMEDAD, "Insuficiencia cardíaca")),
                        TipoClinico.MEDICAMENTO,
                        List.of(new VocabularyTerm(TipoClinico.MEDICAMENTO, "Losartán"))));
    }

    private ArticleAnalysisResult successResult(UUID specialtyId) {
        return new ArticleAnalysisResult(
                specialtyId,
                List.of(new ExtractedTag(TipoClinico.ENFERMEDAD, "Insuficiencia cardíaca")),
                "llama-3.3-70b-versatile", 600, 80, true);
    }

    // ------------------------------------------------------------------ tests

    @Test
    @DisplayName("Happy path: 2 artículos sin especialidad → gateway llamado por cada uno → succeeded=2")
    void happyPathTwoArticles() {
        UUID specialtyId = UUID.randomUUID();
        Article a1 = articleWithText("Artículo sobre cardio", "resumen cardio", "cardio");
        Article a2 = articleWithText("Artículo sobre neuro", "resumen neuro", "neuro");

        when(articleRepository.findAllWithoutSpecialty()).thenReturn(List.of(a1, a2));
        when(specialtyRepository.findAllActive()).thenReturn(List.of(cardiologia(specialtyId)));
        when(vocabularyRepository.getVocabularyFor(specialtyId)).thenReturn(cardioVocab(specialtyId));
        when(aiGateway.analyzeArticle(any(ArticleAnalysisRequest.class))).thenReturn(successResult(specialtyId));

        AutoAnalysisSummary summary = serviceEnabled().execute();

        assertEquals(2, summary.attempted());
        assertEquals(2, summary.succeeded());
        assertEquals(0, summary.skippedBlank());
        assertEquals(0, summary.failed());
        verify(aiGateway, times(2)).analyzeArticle(any());
        verify(saver, times(2)).save(any(), any(), any());
    }

    @Test
    @DisplayName("Sin candidatos: repo devuelve lista vacía → summary todo ceros, gateway nunca llamado")
    void noCandidates() {
        when(articleRepository.findAllWithoutSpecialty()).thenReturn(List.of());

        AutoAnalysisSummary summary = serviceEnabled().execute();

        assertEquals(0, summary.attempted());
        assertEquals(0, summary.succeeded());
        assertEquals(0, summary.skippedBlank());
        assertEquals(0, summary.failed());
        verifyNoInteractions(aiGateway, saver);
    }

    @Test
    @DisplayName("Artículo con todos los campos de texto en blanco → skippedBlank=1, gateway no llamado para ese artículo")
    void blankTextArticleIsSkipped() {
        UUID specialtyId = UUID.randomUUID();
        // Only "titulo" is enforced non-blank by the domain, so we spy to fake a blank titulo
        Article blank = spy(articleWithText("Título placeholder", null, null));
        when(blank.getTitulo()).thenReturn("   ");
        when(blank.getAbstractText()).thenReturn(null);
        when(blank.getKeywords()).thenReturn("");

        Article good = articleWithText("Buen artículo con titulo", "resumen", null);

        when(articleRepository.findAllWithoutSpecialty()).thenReturn(List.of(blank, good));
        when(specialtyRepository.findAllActive()).thenReturn(List.of(cardiologia(specialtyId)));
        when(vocabularyRepository.getVocabularyFor(specialtyId)).thenReturn(cardioVocab(specialtyId));
        when(aiGateway.analyzeArticle(any())).thenReturn(successResult(specialtyId));

        AutoAnalysisSummary summary = serviceEnabled().execute();

        assertEquals(2, summary.attempted());
        assertEquals(1, summary.succeeded());
        assertEquals(1, summary.skippedBlank());
        assertEquals(0, summary.failed());
        // gateway called only once — for the good article
        verify(aiGateway, times(1)).analyzeArticle(any());
    }

    @Test
    @DisplayName("Gateway lanza AiAnalysisException en un artículo → failed=1, otros artículos continúan, no se propaga excepción")
    void gatewayExceptionIsCaughtAndCounted() {
        UUID specialtyId = UUID.randomUUID();
        Article a1 = articleWithText("Artículo 1", "resumen 1", null);
        Article a2 = articleWithText("Artículo 2", "resumen 2", null);

        when(articleRepository.findAllWithoutSpecialty()).thenReturn(List.of(a1, a2));
        when(specialtyRepository.findAllActive()).thenReturn(List.of(cardiologia(specialtyId)));
        when(vocabularyRepository.getVocabularyFor(specialtyId)).thenReturn(cardioVocab(specialtyId));
        when(aiGateway.analyzeArticle(any()))
                .thenThrow(new AiAnalysisException("Groq error"))
                .thenReturn(successResult(specialtyId));

        AutoAnalysisSummary summary = assertDoesNotThrow(() -> serviceEnabled().execute());

        assertEquals(2, summary.attempted());
        assertEquals(1, summary.succeeded());
        assertEquals(0, summary.skippedBlank());
        assertEquals(1, summary.failed());
    }

    @Test
    @DisplayName("Gateway lanza AiAnalysisTimeoutException → contado como failed, no se propaga")
    void timeoutExceptionIsCaughtAndCounted() {
        UUID specialtyId = UUID.randomUUID();
        Article a1 = articleWithText("Artículo timeout", "resumen", null);

        when(articleRepository.findAllWithoutSpecialty()).thenReturn(List.of(a1));
        when(specialtyRepository.findAllActive()).thenReturn(List.of(cardiologia(specialtyId)));
        when(vocabularyRepository.getVocabularyFor(specialtyId)).thenReturn(cardioVocab(specialtyId));
        when(aiGateway.analyzeArticle(any())).thenThrow(new AiAnalysisTimeoutException("timeout"));

        AutoAnalysisSummary summary = assertDoesNotThrow(() -> serviceEnabled().execute());

        assertEquals(1, summary.failed());
        assertEquals(0, summary.succeeded());
        verify(saver, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("Feature disabled → execute() devuelve todos ceros, findAllWithoutSpecialty nunca llamado")
    void featureDisabledSkipsEverything() {
        AutoAnalysisSummary summary = serviceDisabled().execute();

        assertEquals(0, summary.attempted());
        assertEquals(0, summary.succeeded());
        assertEquals(0, summary.skippedBlank());
        assertEquals(0, summary.failed());
        verifyNoInteractions(articleRepository, aiGateway, saver);
    }

    @Test
    @DisplayName("Specialties y vocabularies cargados una sola vez para 3 artículos candidatos")
    void specialtiesAndVocabsLoadedOnce() {
        UUID specialtyId = UUID.randomUUID();
        Article a1 = articleWithText("A1", "r1", null);
        Article a2 = articleWithText("A2", "r2", null);
        Article a3 = articleWithText("A3", "r3", null);

        when(articleRepository.findAllWithoutSpecialty()).thenReturn(List.of(a1, a2, a3));
        when(specialtyRepository.findAllActive()).thenReturn(List.of(cardiologia(specialtyId)));
        when(vocabularyRepository.getVocabularyFor(specialtyId)).thenReturn(cardioVocab(specialtyId));
        when(aiGateway.analyzeArticle(any())).thenReturn(successResult(specialtyId));

        serviceEnabled().execute();

        // findAllActive() y getVocabularyFor() se llaman exactamente una vez, sin importar cuántos artículos
        verify(specialtyRepository, times(1)).findAllActive();
        verify(vocabularyRepository, times(1)).getVocabularyFor(specialtyId);
    }
}
