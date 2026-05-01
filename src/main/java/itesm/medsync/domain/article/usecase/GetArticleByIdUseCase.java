package itesm.medsync.domain.article.usecase;

import itesm.medsync.domain.article.model.Article;

import java.util.UUID;

public interface GetArticleByIdUseCase {

    Article execute(UUID id);
}
