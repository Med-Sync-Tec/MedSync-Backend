package itesm.medsync.domain.article.usecase;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.shared.model.Page;

import java.util.UUID;

public interface GetSavedArticlesUseCase {
    /** Obtiene los artículos guardados por el usuario, paginados. */
    Page<Article> execute(UUID userId, int page, int size);
}
