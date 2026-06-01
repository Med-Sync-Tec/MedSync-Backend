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

    boolean existsByUrl(String url);

    List<Article> findMatchingArticlesForPaciente(UUID pacienteId, int limit);

    /**
     * Devuelve los artículos cuyo tag de tipo MEDICAMENTO coincide (normalizado:
     * sin distinguir mayúsculas y recortando espacios) con el nombre de algún
     * medicamento del catálogo. Es el análogo del match por paciente, pero usando
     * todo el catálogo de medicamentos como "contexto" del COO.
     */
    List<Article> findMatchingArticlesForMedicamentos(int limit);

    /**
     * Devuelve los artículos ordenados por fecha de actualización descendente
     * (los más recientes primero), con paginación.
     */
    Page<Article> findRecentArticles(int page, int size);

    /**
     * Devuelve los artículos guardados por un usuario, con paginación.
     */
    Page<Article> findSavedArticles(UUID userId, int page, int size);
    /**
     * Returns all articles that have no specialty assigned yet
     * ({@code especialidad_id IS NULL}), ordered by {@code createdAt} ascending.
     *
     * <p>Only live (non-soft-deleted) articles are returned. The ascending order
     * lets a restarted pass continue from roughly where it left off, since
     * already-classified articles are excluded by the {@code IS NULL} filter.
     */
    List<Article> findAllWithoutSpecialty();
}

