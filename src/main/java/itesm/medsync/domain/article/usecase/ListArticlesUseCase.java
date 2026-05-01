package itesm.medsync.domain.article.usecase;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.shared.model.Page;

public interface ListArticlesUseCase {

    Page<Article> execute(int page, int size);
}
