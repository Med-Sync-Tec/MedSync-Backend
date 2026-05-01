package itesm.medsync.application.article;

import itesm.medsync.domain.article.exception.ArticleNotFoundException;
import itesm.medsync.domain.article.exception.ArticleTagNotFoundException;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.RemoveTagFromArticleUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class RemoveTagFromArticleService implements RemoveTagFromArticleUseCase {

    private final ArticleRepository repository;

    @Inject
    public RemoveTagFromArticleService(ArticleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID articleId, UUID tagId) {
        Article article = repository.findByUuid(articleId)
                .orElseThrow(() -> new ArticleNotFoundException(articleId));
        boolean tagExists = article.getTags().stream()
                .anyMatch(t -> t.getId().equals(tagId));
        if (!tagExists) {
            throw new ArticleTagNotFoundException(tagId);
        }
        Article updated = article.withTagRemoved(tagId);
        repository.save(updated);
    }
}
