package itesm.medsync.domain.solicitud.exception;

public class SolicitudNotFoundException extends RuntimeException {
    public SolicitudNotFoundException(String token) {
        super("Solicitud no encontrada: " + token);
    }
}
