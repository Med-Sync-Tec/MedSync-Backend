package itesm.medsync.domain.article.usecase;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.shared.model.Page;

/**
 * Caso de uso: obtener los artículos más recientes (ordenados por fecha de
 * actualización descendente) para mostrar en el home del médico.
 */
public interface GetRecentArticlesUseCase {

    Page<Article> execute(int page, int size);
}
