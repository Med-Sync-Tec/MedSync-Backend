package itesm.medsync.domain.medicamento.model;

import java.util.Objects;
import java.util.UUID;

public record MedicamentoEstado(UUID id, String nombre, String descripcion) {

    // Compact constructor handles all validation
    public MedicamentoEstado {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre cannot be blank");
    }

    // Backward-compatible accessors
    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }

    // Id-based equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicamentoEstado other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
