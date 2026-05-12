package itesm.medsync.infrastructure.persistence.tracking;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UsuarioArticuloLeidoId implements Serializable {
    private UUID usuario;
    private UUID articulo;

    public UsuarioArticuloLeidoId() {}

    public UsuarioArticuloLeidoId(UUID usuario, UUID articulo) {
        this.usuario = usuario;
        this.articulo = articulo;
    }

    public UUID getUsuario() {
        return usuario;
    }

    public void setUsuario(UUID usuario) {
        this.usuario = usuario;
    }

    public UUID getArticulo() {
        return articulo;
    }

    public void setArticulo(UUID articulo) {
        this.articulo = articulo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioArticuloLeidoId that = (UsuarioArticuloLeidoId) o;
        return Objects.equals(usuario, that.usuario) && Objects.equals(articulo, that.articulo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuario, articulo);
    }
}
