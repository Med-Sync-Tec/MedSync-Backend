package itesm.medsync.domain.article.repository;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.shared.model.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository {

    Article save(Article article);

    Optional<Article> findByUuid(UUID id);

    Page<Article> listArticles(int page, int size);

    boolean existsByDoi(String doi);

    List<Article> findMatchingArticlesForPaciente(UUID pacienteId, int limit);
}
