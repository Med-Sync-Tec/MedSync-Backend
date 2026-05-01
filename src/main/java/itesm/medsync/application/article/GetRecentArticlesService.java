package itesm.medsync.application.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.GetRecentArticlesUseCase;
import itesm.medsync.domain.shared.model.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetRecentArticlesService implements GetRecentArticlesUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ArticleRepository repository;

    @Inject
    public GetRecentArticlesService(ArticleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<Article> execute(int page, int size) {
        int safeSize = (size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int safePage = Math.max(0, page);
        return repository.findRecentArticles(safePage, safeSize);
    }
}
