package itesm.medsync.interfaces.rest.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import itesm.medsync.interfaces.rest.article.ArticleResponse;

import java.util.List;

/**
 * REST response envelope for {@code GET /api/v1/dashboard/kpis}.
 *
 * Holds {@link ArticleResponse} (the standard article projection used by
 * every other endpoint) instead of JPA entities, so Jackson never sees the
 * {@code ArticleEntity ↔ ArticleTagEntity} back-reference that previously
 * caused a 1000-depth recursion crash. The field names match the legacy
 * shape so the frontend's existing types continue to work unchanged.
 */
public class DashboardKpiDTO {
    @JsonProperty("novedades_48h")
    public List<ArticleResponse> novedades48h;

    @JsonProperty("no_leidos")
    public List<ArticleResponse> noLeidos;

    @JsonProperty("por_especialidad")
    public List<ArticleResponse> porEspecialidad;

    @JsonProperty("alta_evidencia")
    public List<ArticleResponse> altaEvidencia;

    public DashboardKpiDTO(List<ArticleResponse> novedades48h,
                           List<ArticleResponse> noLeidos,
                           List<ArticleResponse> porEspecialidad,
                           List<ArticleResponse> altaEvidencia) {
        this.novedades48h = novedades48h;
        this.noLeidos = noLeidos;
        this.porEspecialidad = porEspecialidad;
        this.altaEvidencia = altaEvidencia;
    }
}
