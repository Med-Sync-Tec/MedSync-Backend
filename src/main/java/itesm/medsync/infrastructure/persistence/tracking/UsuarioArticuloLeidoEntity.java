package itesm.medsync.infrastructure.persistence.tracking;

import itesm.medsync.infrastructure.persistence.article.ArticleEntity;
import itesm.medsync.infrastructure.persistence.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_articulos_leidos")
@IdClass(UsuarioArticuloLeidoId.class)
public class UsuarioArticuloLeidoEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", columnDefinition = "BINARY(16)", nullable = false)
    private UserEntity usuario;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "articulo_id", columnDefinition = "BINARY(16)", nullable = false)
    private ArticleEntity articulo;

    @CreationTimestamp
    @Column(name = "leido_at", updatable = false)
    private LocalDateTime leidoAt;

    public UsuarioArticuloLeidoEntity() {
        // Required by Hibernate
    }

    public UserEntity getUsuario() {
        return usuario;
    }

    public void setUsuario(UserEntity usuario) {
        this.usuario = usuario;
    }

    public ArticleEntity getArticulo() {
        return articulo;
    }

    public void setArticulo(ArticleEntity articulo) {
        this.articulo = articulo;
    }

    public LocalDateTime getLeidoAt() {
        return leidoAt;
    }

    public void setLeidoAt(LocalDateTime leidoAt) {
        this.leidoAt = leidoAt;
    }
}
