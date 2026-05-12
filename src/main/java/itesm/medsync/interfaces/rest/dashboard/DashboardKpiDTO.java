package itesm.medsync.interfaces.rest.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;

import itesm.medsync.infrastructure.persistence.article.ArticleEntity;

import java.util.List;

public class DashboardKpiDTO {
    @JsonProperty("novedades_48h")
    public List<ArticleEntity> novedades48h;

    @JsonProperty("no_leidos")
    public List<ArticleEntity> noLeidos;

    @JsonProperty("por_especialidad")
    public List<ArticleEntity> porEspecialidad;

    @JsonProperty("alta_evidencia")
    public List<ArticleEntity> altaEvidencia;
    
    public DashboardKpiDTO(List<ArticleEntity> novedades48h, List<ArticleEntity> noLeidos, List<ArticleEntity> porEspecialidad, List<ArticleEntity> altaEvidencia) {
        this.novedades48h = novedades48h;
        this.noLeidos = noLeidos;
        this.porEspecialidad = porEspecialidad;
        this.altaEvidencia = altaEvidencia;
    }
}
