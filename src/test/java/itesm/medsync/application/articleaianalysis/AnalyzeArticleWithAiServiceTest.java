package itesm.medsync.application.articleaianalysis;

import itesm.medsync.domain.article.exception.ArticleNotFoundException;
import itesm.medsync.domain.article.exception.InvalidArticleDataException;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisException;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisTimeoutException;
import itesm.medsync.domain.articleaianalysis.model.AnalyzedArticle;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyzeArticleWithAiServiceTest {

    @Mock
    ArticleRepository articleRepository;

    @Mock
    SpecialtyRepository specialtyRepository;

    @Mock
    VocabularyRepository vocabularyRepository;

    @Mock
    AiAnalysisGateway aiGateway;

    @InjectMocks
    AnalyzeArticleWithAiService service;

    private Article articleWithText(String titulo, String abs, String keywords) {
        return Article.create(
                titulo == null ? "Título genérico" : titulo,
                "Autores", "Revista", 2024, "Mar",
                "10.1234/abc",
                abs,
                keywords,
                "Journal Article",
                "https://doi.org/10.1234/abc");
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

    @Test
    @DisplayName("Happy path: artículo con abstract; gateway devuelve tags válidos; se guarda con especialidad reemplazada")
    void analyzeHappyPath() {
        UUID articleId = UUID.randomUUID();
        UUID specialtyId = UUID.randomUUID();
        Article article = articleWithText(
                "Tratamiento de insuficiencia cardíaca",
                "Resumen sobre IC y losartán...",
                "insuficiencia cardiaca, losartan");
        when(articleRepository.findByUuid(articleId)).thenReturn(Optional.of(article));
        when(specialtyRepository.findAllActive()).thenReturn(List.of(cardiologia(specialtyId)));
        when(vocabularyRepository.getVocabularyFor(specialtyId)).thenReturn(cardioVocab(specialtyId));

        ArticleAnalysisResult gatewayResult = new ArticleAnalysisResult(
                specialtyId,
                List.of(
                        new ExtractedTag(TipoClinico.ENFERMEDAD, "Insuficiencia cardíaca"),
                        new ExtractedTag(TipoClinico.MEDICAMENTO, "Losartán")),
                "llama-3.3-70b-versatile", 800, 120, true);
        when(aiGateway.analyzeArticle(any(ArticleAnalysisRequest.class))).thenReturn(gatewayResult);
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        // mock the id-lookup for the chosen specialty
        when(articleRepository.findByUuid(articleId)).thenReturn(Optional.of(article));

        AnalyzedArticle result = service.execute(articleId);

        assertNotNull(result);
        assertEquals(specialtyId, result.article().getEspecialidadId());
        assertEquals(2, result.article().getTags().size());
        assertTrue(result.analysis().hadAbstract());

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).save(captor.capture());
        Article saved = captor.getValue();
        assertEquals(specialtyId, saved.getEspecialidadId());
        assertEquals(2, saved.getTags().size());
    }

    @Test
    @DisplayName("Artículo no existe → ArticleNotFoundException, gateway no es llamado")
    void notFound() {
        UUID articleId = UUID.randomUUID();
        when(articleRepository.findByUuid(articleId)).thenReturn(Optional.empty());

        assertThrows(ArticleNotFoundException.class, () -> service.execute(articleId));
        verifyNoInteractions(aiGateway);
        verify(articleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sin abstract pero con título → llama al gateway con hasAbstract=false; guarda normalmente")
    void degradedPathNoAbstract() {
        UUID articleId = UUID.randomUUID();
        UUID specialtyId = UUID.randomUUID();
        Article article = articleWithText(
                "Trastuzumab deruxtecan in HER2-low metastatic breast cancer",
                null,
                "her2-low, breast cancer");
        when(articleRepository.findByUuid(articleId)).thenReturn(Optional.of(article));
        when(specialtyRepository.findAllActive()).thenReturn(List.of(cardiologia(specialtyId)));
        when(vocabularyRepository.getVocabularyFor(specialtyId)).thenReturn(cardioVocab(specialtyId));

        ArticleAnalysisResult gatewayResult = new ArticleAnalysisResult(
                specialtyId,
                List.of(new ExtractedTag(TipoClinico.ENFERMEDAD, "Insuficiencia cardíaca")),
                "llama-3.3-70b-versatile", 400, 60, false);
        ArgumentCaptor<ArticleAnalysisRequest> requestCaptor =
                ArgumentCaptor.forClass(ArticleAnalysisRequest.class);
        when(aiGateway.analyzeArticle(requestCaptor.capture())).thenReturn(gatewayResult);
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        AnalyzedArticle result = service.execute(articleId);

        assertFalse(result.analysis().hadAbstract());
        assertFalse(requestCaptor.getValue().hasAbstract());
        verify(articleRepository).save(any(Article.class));
    }

    @Test
    @DisplayName("Solo título no-blank (sin abstract ni keywords) → analiza igual con hadAbstract=false")
    void titleOnly() {
        UUID articleId = UUID.randomUUID();
        UUID specialtyId = UUID.randomUUID();
        Article article = articleWithText(
                "Editorial: New trends in cardiology",
                null,
                null);
        when(articleRepository.findByUuid(articleId)).thenReturn(Optional.of(article));
        when(specialtyRepository.findAllActive()).thenReturn(List.of(cardiologia(specialtyId)));
        when(vocabularyRepository.getVocabularyFor(specialtyId)).thenReturn(cardioVocab(specialtyId));

        ArticleAnalysisResult gatewayResult = new ArticleAnalysisResult(
                specialtyId,
                List.of(new ExtractedTag(TipoClinico.MEDICAMENTO, "Losartán")),
                "llama-3.3-70b-versatile", 300, 50, false);
        when(aiGateway.analyzeArticle(any(ArticleAnalysisRequest.class))).thenReturn(gatewayResult);
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        AnalyzedArticle result = service.execute(articleId);

        assertFalse(result.analysis().hadAbstract());
        assertEquals(1, result.article().getTags().size());
    }

    @Test
    @DisplayName("Artículo cuyo titulo+abstract+keywords colapsan a blank → InvalidArticleDataException; gateway no llamado")
    void allBlankRejected() {
        UUID articleId = UUID.randomUUID();
        // Article enforces non-blank titulo at construction, so we simulate the
        // "structurally empty" case via a Mockito spy that reports blank for all
        // three analyzable fields. This exercises the service's defensive check
        // without requiring DB corruption.
        Article article = articleWithText("Título placeholder", null, null);
        Article blanksSpy = spy(article);
        when(blanksSpy.getTitulo()).thenReturn("   ");
        when(blanksSpy.getAbstractText()).thenReturn(null);
        when(blanksSpy.getKeywords()).thenReturn("");
        when(articleRepository.findByUuid(articleId)).thenReturn(Optional.of(blanksSpy));

        assertThrows(InvalidArticleDataException.class, () -> service.execute(articleId));
        verifyNoInteractions(aiGateway);
        verify(articleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Gateway lanza AiAnalysisException → se propaga; no se guarda")
    void gatewayAnalysisExceptionPropagates() {
        UUID articleId = UUID.randomUUID();
        UUID specialtyId = UUID.randomUUID();
        Article article = articleWithText("titulo", "abstract", "keywords");
        when(articleRepository.findByUuid(articleId)).thenReturn(Optional.of(article));
        when(specialtyRepository.findAllActive()).thenReturn(List.of(cardiologia(specialtyId)));
        when(vocabularyRepository.getVocabularyFor(specialtyId)).thenReturn(cardioVocab(specialtyId));
        when(aiGateway.analyzeArticle(any())).thenThrow(new AiAnalysisException("boom"));

        assertThrows(AiAnalysisException.class, () -> service.execute(articleId));
        verify(articleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Gateway lanza AiAnalysisTimeoutException → se propaga; no se guarda")
    void gatewayTimeoutPropagates() {
        UUID articleId = UUID.randomUUID();
        UUID specialtyId = UUID.randomUUID();
        Article article = articleWithText("titulo", "abstract", "keywords");
        when(articleRepository.findByUuid(articleId)).thenReturn(Optional.of(article));
        when(specialtyRepository.findAllActive()).thenReturn(List.of(cardiologia(specialtyId)));
        when(vocabularyRepository.getVocabularyFor(specialtyId)).thenReturn(cardioVocab(specialtyId));
        when(aiGateway.analyzeArticle(any())).thenThrow(new AiAnalysisTimeoutException("slow"));

        assertThrows(AiAnalysisTimeoutException.class, () -> service.execute(articleId));
        verify(articleRepository, never()).save(any());
    }

    @Test
    @DisplayName("ArticleAnalysisRequest se construye con las especialidades activas y los vocabularios cargados")
    void requestPassedToGateway() {
        UUID articleId = UUID.randomUUID();
        UUID specialtyId = UUID.randomUUID();
        Article article = articleWithText("titulo", "abstract", "keywords");
        when(articleRepository.findByUuid(articleId)).thenReturn(Optional.of(article));
        Specialty s = cardiologia(specialtyId);
        when(specialtyRepository.findAllActive()).thenReturn(List.of(s));
        Vocabulary vocab = cardioVocab(specialtyId);
        when(vocabularyRepository.getVocabularyFor(specialtyId)).thenReturn(vocab);

        ArticleAnalysisResult gatewayResult = new ArticleAnalysisResult(
                specialtyId,
                List.of(new ExtractedTag(TipoClinico.ENFERMEDAD, "Insuficiencia cardíaca")),
                "llama-3.3-70b-versatile", 100, 50, true);
        ArgumentCaptor<ArticleAnalysisRequest> captor =
                ArgumentCaptor.forClass(ArticleAnalysisRequest.class);
        when(aiGateway.analyzeArticle(captor.capture())).thenReturn(gatewayResult);
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        service.execute(articleId);

        ArticleAnalysisRequest passed = captor.getValue();
        assertEquals(1, passed.candidateSpecialties().size());
        assertEquals(specialtyId, passed.candidateSpecialties().get(0).id());
        assertEquals("Cardiología", passed.candidateSpecialties().get(0).nombre());
        assertSame(vocab, passed.vocabulariesById().get(specialtyId));
    }
}
