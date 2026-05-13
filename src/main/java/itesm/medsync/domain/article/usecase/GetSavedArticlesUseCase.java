package itesm.medsync.domain.article.usecase;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.shared.model.Page;

import java.util.UUID;

public interface GetSavedArticlesUseCase {
    Page<Article> execute(UUID userId, int page, int size);
}
