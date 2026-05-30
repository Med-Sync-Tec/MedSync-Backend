package itesm.medsync.application.pubmedautospecialty;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.repository.ArticleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;

/**
 * Persists the AI-assigned specialty and tags for a single article in its own
 * {@code REQUIRES_NEW} transaction.
 *
 * <p>Keeping the save isolated means that a gateway failure on article N does
 * not roll back the already-committed result for article N-1. The AI gateway
 * call is intentionally performed <em>outside</em> this class — no DB
 * connection is held during the HTTP round-trip to Groq.
 *
 * <p>Package-private: only {@link AutoAnalyzeNewArticlesService} should use this.
 */
@ApplicationScoped
class ArticleSpecialtySaver {

    private final ArticleRepository articleRepository;

    @Inject
    ArticleSpecialtySaver(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    /**
     * Applies {@link Article#withAiAnalysis(UUID, List)} and persists the result
     * in a brand-new transaction.
     */
    @Transactional(REQUIRES_NEW)
    void save(Article article, UUID especialidadId, List<ArticleTag> tags) {
        Article toSave = article.withAiAnalysis(especialidadId, tags);
        articleRepository.save(toSave);
    }
}
