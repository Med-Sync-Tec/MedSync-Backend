package itesm.medsync.application.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.GetMatchingArticlesForMedicamentosUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class GetMatchingArticlesForMedicamentosService implements GetMatchingArticlesForMedicamentosUseCase {

    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 50;

    private final ArticleRepository articleRepository;

    @Inject
    public GetMatchingArticlesForMedicamentosService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public List<Article> execute(int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return articleRepository.findMatchingArticlesForMedicamentos(safeLimit);
    }
}
