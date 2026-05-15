package itesm.medsync.domain.articleaianalysis.model;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.shared.model.ArticleAnalysisResult;

/**
 * Carries the persisted {@link Article} and the upstream gateway's
 * {@link ArticleAnalysisResult} side by side so the REST layer can render
 * both the saved state and provenance metadata (model used, token usage,
 * abstract availability) in one response.
 */
public record AnalyzedArticle(Article article, ArticleAnalysisResult analysis) {
}
