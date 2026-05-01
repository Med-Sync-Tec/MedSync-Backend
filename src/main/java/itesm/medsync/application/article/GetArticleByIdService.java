package itesm.medsync.application.article;

import itesm.medsync.domain.article.exception.ArticleNotFoundException;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.GetArticleByIdUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class GetArticleByIdService implements GetArticleByIdUseCase {

    private final ArticleRepository repository;

    @Inject
    public GetArticleByIdService(ArticleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Article execute(UUID id) {
        return repository.findByUuid(id)
                .orElseThrow(() -> new ArticleNotFoundException(id));
    }
}
