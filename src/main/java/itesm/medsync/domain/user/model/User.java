package itesm.medsync.domain.user.model;

import itesm.medsync.domain.user.exception.InvalidUserDataException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record User(
        UUID id,
        String nombre,
        String correo,
        UUID especialidadId,
        UUID rolId,
        boolean activo,
        LocalDateTime createdAt) {

    // Compact constructor handles all validation
    public User {
        if (id == null) {
            throw new InvalidUserDataException("User id cannot be null");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new InvalidUserDataException("nombre cannot be null or blank");
        }
        if (correo == null || correo.isBlank()) {
            throw new InvalidUserDataException("correo cannot be null or blank");
        }
        if (!correo.contains("@") || correo.indexOf('@') == 0 || correo.indexOf('@') == correo.length() - 1) {
            throw new InvalidUserDataException("correo must be a valid email address");
        }
        if (rolId == null) {
            throw new InvalidUserDataException("rolId cannot be null");
        }
    }

    public static User create(String nombre, String correo, UUID especialidadId, UUID rolId) {
        return new User(UUID.randomUUID(), nombre, correo, especialidadId, rolId, true, null);
    }

    public User deactivate() {
        return new User(id, nombre, correo, especialidadId, rolId, false, createdAt);
    }

    // Backward-compatible accessors
    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public UUID getEspecialidadId() {
        return especialidadId;
    }

    public UUID getRolId() {
        return rolId;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Id-based equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
