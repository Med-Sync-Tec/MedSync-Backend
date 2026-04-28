package itesm.medsync.application.article;

import itesm.medsync.domain.article.exception.ArticleNotFoundException;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.AddTagToArticleUseCase;
import itesm.medsync.domain.shared.model.TipoClinico;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class AddTagToArticleService implements AddTagToArticleUseCase {

    private final ArticleRepository repository;

    @Inject
    public AddTagToArticleService(ArticleRepository repository) {
        this.repository = repository;
    }

    @Override
    public ArticleTag execute(UUID articleId, TipoClinico tipo, String valor) {
        Article article = repository.findByUuid(articleId)
                .orElseThrow(() -> new ArticleNotFoundException(articleId));
        ArticleTag tag = ArticleTag.create(tipo, valor);
        Article updated = article.withTagAdded(tag);
        Article saved = repository.save(updated);
        return saved.getTags().stream()
                .filter(t -> t.getId().equals(tag.getId()))
                .findFirst()
                .orElse(tag);
    }
}
