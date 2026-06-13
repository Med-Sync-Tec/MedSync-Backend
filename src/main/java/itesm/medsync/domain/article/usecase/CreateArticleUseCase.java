package itesm.medsync.domain.article.usecase;

import itesm.medsync.domain.article.model.Article;

public interface CreateArticleUseCase {

    Article execute(CreateArticleCommand cmd);
}
