package itesm.medsync.infrastructure.persistence.article;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "articulos_cientificos")
@NamedEntityGraph(
        name = "Article.withTags",
        attributeNodes = { @NamedAttributeNode("tags") }
)
public class ArticleEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 1000)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String autores;

    @Column(length = 500)
    private String revista;

    @Column(name = "anio_pub")
    private Integer anioPub;

    @Column(name = "mes_pub", length = 20)
    private String mesPub;

    @Column(length = 200)
    private String doi;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "tipo_publicacion", length = 100)
    private String tipoPublicacion;

    @Column(length = 500)
    private String url;

    @Column(name = "especialidad_id", columnDefinition = "BINARY(16)")
    private UUID especialidadId;

    @OneToMany(mappedBy = "articulo",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<ArticleTagEntity> tags = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ArticleEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutores() {
        return autores;
    }

    public void setAutores(String autores) {
        this.autores = autores;
    }

    public String getRevista() {
        return revista;
    }

    public void setRevista(String revista) {
        this.revista = revista;
    }

    public Integer getAnioPub() {
        return anioPub;
    }

    public void setAnioPub(Integer anioPub) {
        this.anioPub = anioPub;
    }

    public String getMesPub() {
        return mesPub;
    }

    public void setMesPub(String mesPub) {
        this.mesPub = mesPub;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getTipoPublicacion() {
        return tipoPublicacion;
    }

    public void setTipoPublicacion(String tipoPublicacion) {
        this.tipoPublicacion = tipoPublicacion;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UUID getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(UUID especialidadId) {
        this.especialidadId = especialidadId;
    }

    public List<ArticleTagEntity> getTags() {
        return tags;
    }

    public void setTags(List<ArticleTagEntity> tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
