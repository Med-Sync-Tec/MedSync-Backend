package itesm.medsync.domain.article.usecase;

import itesm.medsync.domain.article.model.Article;

import java.util.List;

/**
 * Lista los artículos cuyo tag de tipo MEDICAMENTO coincide con algún medicamento
 * del catálogo. Es el match del COO: usa todo el catálogo como contexto, en vez
 * del contexto clínico de un paciente.
 */
public interface GetMatchingArticlesForMedicamentosUseCase {

    List<Article> execute(int limit);
}
