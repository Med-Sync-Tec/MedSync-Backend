package itesm.medsync.application.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.GetSavedArticlesUseCase;
import itesm.medsync.domain.shared.model.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class GetSavedArticlesService implements GetSavedArticlesUseCase {

    private final ArticleRepository articleRepository;

    @Inject
    public GetSavedArticlesService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public Page<Article> execute(UUID userId, int page, int size) {
        return articleRepository.findSavedArticles(userId, page, size);
    }
}
