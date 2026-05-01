package itesm.medsync.domain.article.usecase;

import itesm.medsync.domain.article.model.Article;

public interface CreateArticleUseCase {

    Article execute(String titulo,
                    String autores,
                    String revista,
                    Integer anioPub,
                    String mesPub,
                    String doi,
                    String abstractText,
                    String keywords,
                    String tipoPublicacion,
                    String url);
}
