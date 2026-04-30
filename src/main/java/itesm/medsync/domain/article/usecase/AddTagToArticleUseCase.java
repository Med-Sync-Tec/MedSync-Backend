package itesm.medsync.domain.article.usecase;

import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.shared.model.TipoClinico;

import java.util.UUID;

public interface AddTagToArticleUseCase {

    ArticleTag execute(UUID articleId, TipoClinico tipo, String valor);
}
