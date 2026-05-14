package itesm.medsync.domain.shared.repository;

import itesm.medsync.domain.shared.model.ArticleAnalysisRequest;
import itesm.medsync.domain.shared.model.ArticleAnalysisResult;
import itesm.medsync.domain.shared.model.ConsultaAnalysisRequest;
import itesm.medsync.domain.shared.model.ConsultaAnalysisResult;

/**
 * Provider-agnostic port for AI-backed clinical analysis.
 *
 * Two methods because both AI features share authentication, error mapping,
 * timeout policy, and observability — splitting them into separate ports
 * would force callers to know about the classify/extract decomposition that
 * is purely an implementation detail of the Groq-backed adapter.
 *
 * Lives in {@code domain/shared/repository} because feature 4
 * ({@code consulta-ai-analysis}) reuses the same port; per the project's
 * gateway-vs-repository convention, this is a {@code *Gateway} because the
 * data lives in an external HTTP system.
 */
public interface AiAnalysisGateway {

    ArticleAnalysisResult analyzeArticle(ArticleAnalysisRequest request);

    ConsultaAnalysisResult analyzeConsultaText(ConsultaAnalysisRequest request);
}
