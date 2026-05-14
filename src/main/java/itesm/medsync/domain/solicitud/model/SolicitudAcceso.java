package itesm.medsync.domain.solicitud.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class SolicitudAcceso {

    private final UUID id;
    private final String nombre;
    private final String correo;
    private final String rol;
    private EstadoSolicitud estado;
    private final String token;
    private final LocalDateTime createdAt;
    private LocalDateTime rejectedAt;

    public SolicitudAcceso(UUID id, String nombre, String correo, String rol,
                           EstadoSolicitud estado, String token,
                           LocalDateTime createdAt, LocalDateTime rejectedAt) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
        this.estado = estado;
        this.token = token;
        this.createdAt = createdAt;
        this.rejectedAt = rejectedAt;
    }

    public static SolicitudAcceso create(String nombre, String correo, String rol) {
        return new SolicitudAcceso(
                UUID.randomUUID(), nombre, correo, rol,
                EstadoSolicitud.PENDIENTE, UUID.randomUUID().toString(),
                LocalDateTime.now(), null
        );
    }

    public void aprobar() {
        this.estado = EstadoSolicitud.APROBADO;
    }

    public void rechazar() {
        this.estado = EstadoSolicitud.RECHAZADO;
        this.rejectedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getRol() { return rol; }
    public EstadoSolicitud getEstado() { return estado; }
    public String getToken() { return token; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
}
