package itesm.medsync.domain.user.model;

import itesm.medsync.domain.user.exception.InvalidRoleDataException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record Role(UUID id, String nombre, String descripcion, LocalDateTime createdAt) {

    // Compact constructor handles all validation
    public Role {
        if (id == null) {
            throw new InvalidRoleDataException("Role id cannot be null");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new InvalidRoleDataException("Role nombre cannot be null or blank");
        }
    }

    public static Role create(String nombre, String descripcion) {
        return new Role(UUID.randomUUID(), nombre, descripcion, null);
    }

    // Backward-compatible accessors
    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Id-based equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
