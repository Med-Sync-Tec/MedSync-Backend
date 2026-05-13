package itesm.medsync.domain.user.model;

import itesm.medsync.domain.user.exception.InvalidUserDataException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class User {

    private final UUID id;
    private final String nombre;
    private final String correo;
    private final String especialidad;
    private final UUID rolId;
    private final boolean activo;
    private final LocalDateTime createdAt;

    public User(UUID id,
                String nombre,
                String correo,
                String especialidad,
                UUID rolId,
                boolean activo,
                LocalDateTime createdAt) {
        validate(id, nombre, correo, rolId);
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.especialidad = especialidad;
        this.rolId = rolId;
        this.activo = activo;
        this.createdAt = createdAt;
    }

    public static User create(String nombre, String correo, String especialidad, UUID rolId) {
        return new User(UUID.randomUUID(), nombre, correo, especialidad, rolId, true, null);
    }

    public User deactivate() {
        return new User(id, nombre, correo, especialidad, rolId, false, createdAt);
    }

    private static void validate(UUID id, String nombre, String correo, UUID rolId) {
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

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getEspecialidad() {
        return especialidad;
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
