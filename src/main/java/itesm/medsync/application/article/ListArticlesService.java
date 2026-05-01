package itesm.medsync.application.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.ListArticlesUseCase;
import itesm.medsync.domain.shared.model.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ListArticlesService implements ListArticlesUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ArticleRepository repository;

    @Inject
    public ListArticlesService(ArticleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<Article> execute(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return repository.listArticles(safePage, safeSize);
    }
}
