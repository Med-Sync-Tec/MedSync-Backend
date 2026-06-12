package itesm.medsync.infrastructure.persistence.article;

import itesm.medsync.domain.shared.model.TipoClinico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "articulo_tags")
public class ArticleTagEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "articulo_id", nullable = false)
    private ArticleEntity articulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoClinico tipo;

    @Column(nullable = false, length = 500)
    private String valor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ArticleTagEntity() {
        // Required by Hibernate
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ArticleEntity getArticulo() {
        return articulo;
    }

    public void setArticulo(ArticleEntity articulo) {
        this.articulo = articulo;
    }

    public TipoClinico getTipo() {
        return tipo;
    }

    public void setTipo(TipoClinico tipo) {
        this.tipo = tipo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
