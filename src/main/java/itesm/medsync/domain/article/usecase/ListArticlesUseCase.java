package itesm.medsync.domain.article.usecase;

import itesm.medsync.domain.article.model.Article;

import java.util.List;

public interface ListArticlesUseCase {

    List<Article> execute();
}
