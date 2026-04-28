package itesm.medsync.domain.article.repository;

import itesm.medsync.domain.article.model.Article;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository {

    Article save(Article article);

    Optional<Article> findByUuid(UUID id);

    List<Article> listAllArticles();

    boolean existsByDoi(String doi);

    List<Article> findMatchingArticlesForPaciente(UUID pacienteId);
}
