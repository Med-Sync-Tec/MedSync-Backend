package itesm.medsync.domain.medicamento.model;

import itesm.medsync.domain.medicamento.exception.InvalidMedicamentoDataException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record Medicamento(
        UUID id,
        String nombre,
        UUID estadoId,
        String descripcion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    // Compact constructor handles all validation
    public Medicamento {
        if (id == null) throw new InvalidMedicamentoDataException("id cannot be null");
        if (nombre == null || nombre.isBlank()) throw new InvalidMedicamentoDataException("nombre cannot be blank");
        if (estadoId == null) throw new InvalidMedicamentoDataException("estadoId cannot be null");
    }

    public static Medicamento create(String nombre, UUID estadoId, String descripcion) {
        return new Medicamento(UUID.randomUUID(), nombre, estadoId, descripcion, null, null);
    }

    public Medicamento withEstado(UUID nuevoEstadoId) {
        return new Medicamento(id, nombre, nuevoEstadoId, descripcion, createdAt, updatedAt);
    }

    public Medicamento update(String nuevoNombre, UUID nuevoEstadoId, String nuevaDescripcion) {
        return new Medicamento(id, nuevoNombre, nuevoEstadoId, nuevaDescripcion, createdAt, updatedAt);
    }

    // Backward-compatible accessors
    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public UUID getEstadoId() { return estadoId; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Id-based equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medicamento other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
