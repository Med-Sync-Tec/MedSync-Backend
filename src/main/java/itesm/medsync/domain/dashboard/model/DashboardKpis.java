package itesm.medsync.domain.dashboard.model;

import itesm.medsync.domain.article.model.Article;

import java.util.List;

/**
 * Aggregate returned by the dashboard use case — four lists of articles, one
 * per KPI bucket the frontend renders.
 *
 * Lives in {@code domain/} because the use case port references it; it
 * carries only the domain {@link Article} model, never JPA entities or REST
 * DTOs (which the original implementation leaked, causing a Jackson
 * recursion crash via the bidirectional {@code ArticleEntity ↔ ArticleTagEntity}
 * back-reference).
 */
public record DashboardKpis(
        List<Article> novedades48h,
        List<Article> noLeidos,
        List<Article> porEspecialidad,
        List<Article> altaEvidencia) {

    public DashboardKpis {
        novedades48h = List.copyOf(novedades48h);
        noLeidos = List.copyOf(noLeidos);
        porEspecialidad = List.copyOf(porEspecialidad);
        altaEvidencia = List.copyOf(altaEvidencia);
    }
}
