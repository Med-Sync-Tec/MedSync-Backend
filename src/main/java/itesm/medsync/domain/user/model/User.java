package itesm.medsync.domain.user.model;

import java.util.UUID;

public class User {
    private UUID id;
    private String nombre;
    private String correo;
    private UUID rolId;
    private boolean activo;

    public User() {}

    public User(UUID id, String nombre, String correo, UUID rolId, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.rolId = rolId;
        this.activo = activo;
    }

    // Getters y Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public UUID getRolId() { return rolId; }
    public void setRolId(UUID rolId) { this.rolId = rolId; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}