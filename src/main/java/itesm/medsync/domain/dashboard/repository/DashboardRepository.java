package itesm.medsync.domain.dashboard.repository;

import itesm.medsync.domain.article.model.Article;

import java.util.List;
import java.util.UUID;

/**
 * Persistence port for the dashboard's KPI queries.
 *
 * Returns the domain {@link Article} model — never a JPA entity or a REST
 * DTO — to keep {@code domain/} free of infrastructure and interface
 * dependencies (per {@code CLAUDE.md}'s dependency rule). The
 * implementation in {@code infrastructure/persistence/dashboard/} maps
 * entities to {@link Article} via {@code ArticlePersistenceMapper}.
 */
public interface DashboardRepository {

    /** Articles created in the last 48 hours, newest first. */
    List<Article> findNovedades48h();

    /** Articles the given user has not yet marked as read. */
    List<Article> findNoLeidos(UUID userId);

    /** Articles whose {@code especialidadId} matches the user's specialty. */
    List<Article> findPorEspecialidad(UUID userId);

    /** Articles classified as high-evidence ({@code tipoPublicacion = "Journal Article"}). */
    List<Article> findAltaEvidencia();
}
