package itesm.medsync.domain.medicamento.model;

import java.util.Objects;
import java.util.UUID;

public final class MedicamentoEstado {

    private final UUID id;
    private final String nombre;
    private final String descripcion;

    public MedicamentoEstado(UUID id, String nombre, String descripcion) {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre cannot be blank");
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicamentoEstado other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
