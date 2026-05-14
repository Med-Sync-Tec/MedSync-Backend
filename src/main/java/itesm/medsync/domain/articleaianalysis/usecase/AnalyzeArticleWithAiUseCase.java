package itesm.medsync.domain.articleaianalysis.usecase;

import itesm.medsync.domain.articleaianalysis.model.AnalyzedArticle;

import java.util.UUID;

/**
 * Triggers Groq-backed classification + tag extraction for an existing article,
 * replacing prior specialty and tags atomically.
 *
 * Returns an {@link AnalyzedArticle} pair so the REST layer can surface
 * observability fields ({@code modelUsed}, {@code promptTokens},
 * {@code completionTokens}, {@code hadAbstract}) that live on the gateway's
 * {@code ArticleAnalysisResult} rather than on the {@code Article} aggregate.
 */
public interface AnalyzeArticleWithAiUseCase {

    AnalyzedArticle execute(UUID articleId);
}
