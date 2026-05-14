package itesm.medsync.application.articleaianalysis;

import itesm.medsync.domain.article.exception.ArticleNotFoundException;
import itesm.medsync.domain.article.exception.InvalidArticleDataException;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.articleaianalysis.model.AnalyzedArticle;
import itesm.medsync.domain.articleaianalysis.usecase.AnalyzeArticleWithAiUseCase;
import itesm.medsync.domain.shared.model.ArticleAnalysisRequest;
import itesm.medsync.domain.shared.model.ArticleAnalysisResult;
import itesm.medsync.domain.shared.model.ExtractedTag;
import itesm.medsync.domain.shared.model.SpecialtyDescriptor;
import itesm.medsync.domain.shared.repository.AiAnalysisGateway;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.repository.VocabularyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the AI-backed article analysis flow.
 *
 * Steps: load article → reject when titulo/abstract/keywords are all blank →
 * load active specialties + their vocabularies → call the AI gateway → apply
 * the result atomically to the article via {@code withAiAnalysis}.
 *
 * Annotated {@code @Transactional} so the read of the article and the write of
 * its replaced tag list happen in one transaction. The Groq round-trips run
 * inside that transaction — see the spec's "AI calls inside the DB transaction"
 * decision for the trade-off.
 */
@ApplicationScoped
@Transactional
public class AnalyzeArticleWithAiService implements AnalyzeArticleWithAiUseCase {

    private final ArticleRepository articleRepository;
    private final SpecialtyRepository specialtyRepository;
    private final VocabularyRepository vocabularyRepository;
    private final AiAnalysisGateway aiGateway;

    @Inject
    public AnalyzeArticleWithAiService(ArticleRepository articleRepository,
                                       SpecialtyRepository specialtyRepository,
                                       VocabularyRepository vocabularyRepository,
                                       AiAnalysisGateway aiGateway) {
        this.articleRepository = articleRepository;
        this.specialtyRepository = specialtyRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.aiGateway = aiGateway;
    }

    @Override
    public AnalyzedArticle execute(UUID articleId) {
        Article article = articleRepository.findByUuid(articleId)
                .orElseThrow(() -> new ArticleNotFoundException(articleId));

        if (isBlank(article.getTitulo())
                && isBlank(article.getAbstractText())
                && isBlank(article.getKeywords())) {
            throw new InvalidArticleDataException(
                    "Article has no analyzable text — titulo, abstractText, and keywords are all blank");
        }

        List<Specialty> activeSpecialties = specialtyRepository.findAllActive();
        List<SpecialtyDescriptor> descriptors = new ArrayList<>(activeSpecialties.size());
        Map<UUID, Vocabulary> vocabsById = new HashMap<>();
        for (Specialty s : activeSpecialties) {
            descriptors.add(new SpecialtyDescriptor(s.getId(), s.getNombre(), s.getSlug(), s.getDescripcion()));
            vocabsById.put(s.getId(), vocabularyRepository.getVocabularyFor(s.getId()));
        }

        ArticleAnalysisRequest gatewayRequest = new ArticleAnalysisRequest(
                article.getTitulo(),
                article.getAbstractText(),
                article.getKeywords(),
                List.copyOf(descriptors),
                Map.copyOf(vocabsById));
        ArticleAnalysisResult result = aiGateway.analyzeArticle(gatewayRequest);

        List<ArticleTag> domainTags = new ArrayList<>(result.tags().size());
        for (ExtractedTag t : result.tags()) {
            domainTags.add(ArticleTag.create(t.tipo(), t.valor()));
        }

        Article toSave = article.withAiAnalysis(result.especialidadId(), domainTags);
        Article saved = articleRepository.save(toSave);
        return new AnalyzedArticle(saved, result);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
