package itesm.medsync.domain.user.model;

import itesm.medsync.domain.user.exception.InvalidRoleDataException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class Role {

    private final UUID id;
    private final String nombre;
    private final String descripcion;
    private final LocalDateTime createdAt;

    public Role(UUID id, String nombre, String descripcion, LocalDateTime createdAt) {
        validate(id, nombre);
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.createdAt = createdAt;
    }

    public static Role create(String nombre, String descripcion) {
        return new Role(UUID.randomUUID(), nombre, descripcion, null);
    }

    private static void validate(UUID id, String nombre) {
        if (id == null) {
            throw new InvalidRoleDataException("Role id cannot be null");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new InvalidRoleDataException("Role nombre cannot be null or blank");
        }
    }

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
