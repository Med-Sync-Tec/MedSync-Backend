package itesm.medsync.application.article;

import itesm.medsync.domain.article.exception.DuplicateArticleException;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.CreateArticleCommand;
import itesm.medsync.domain.article.usecase.CreateArticleUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CreateArticleService implements CreateArticleUseCase {

    private final ArticleRepository repository;

    @Inject
    public CreateArticleService(ArticleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Article execute(CreateArticleCommand cmd) {
        if (cmd.doi() != null && !cmd.doi().isBlank() && repository.existsByDoi(cmd.doi().trim())) {
            throw new DuplicateArticleException(cmd.doi().trim());
        }
        Article article = Article.create(cmd);
        return repository.save(article);
    }
}
