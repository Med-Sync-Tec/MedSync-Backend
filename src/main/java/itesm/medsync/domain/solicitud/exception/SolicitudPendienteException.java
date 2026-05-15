package itesm.medsync.domain.solicitud.exception;

public class SolicitudPendienteException extends RuntimeException {
    public SolicitudPendienteException(String correo) {
        super("Ya existe una solicitud pendiente para el correo: " + correo);
    }
}
