package itesm.medsync.interfaces.rest.articleaianalysis;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.articleaianalysis.model.AnalyzedArticle;
import itesm.medsync.domain.shared.model.ArticleAnalysisResult;
import itesm.medsync.domain.specialty.model.Specialty;

import java.util.List;

/**
 * Maps {@link AnalyzedArticle} (domain) into the REST response envelope.
 *
 * Takes the resolved {@link Specialty} as a parameter so the response carries
 * the human-readable {@code especialidadNombre} alongside the id. The mapper
 * is static — no state, no DI.
 */
public final class AnalyzeArticleRestMapper {

    private AnalyzeArticleRestMapper() {
    }

    public static AnalyzeArticleResponse toResponse(AnalyzedArticle analyzed, Specialty especialidad) {
        Article article = analyzed.article();
        ArticleAnalysisResult result = analyzed.analysis();
        List<AnalyzedTagResponse> tagResponses = article.getTags().stream()
                .map(AnalyzeArticleRestMapper::toTagResponse)
                .toList();
        return new AnalyzeArticleResponse(
                article.getId(),
                article.getEspecialidadId(),
                especialidad == null ? null : especialidad.getNombre(),
                tagResponses,
                result.modelUsed(),
                result.promptTokens(),
                result.completionTokens(),
                result.hadAbstract());
    }

    private static AnalyzedTagResponse toTagResponse(ArticleTag tag) {
        return new AnalyzedTagResponse(tag.getId(), tag.getTipo().name(), tag.getValor());
    }
}
