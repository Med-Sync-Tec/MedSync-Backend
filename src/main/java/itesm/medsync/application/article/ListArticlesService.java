package itesm.medsync.application.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.ListArticlesUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ListArticlesService implements ListArticlesUseCase {

    private final ArticleRepository repository;

    @Inject
    public ListArticlesService(ArticleRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Article> execute() {
        return repository.listAllArticles();
    }
}
