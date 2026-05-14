package itesm.medsync.domain.solicitud.exception;

public class SolicitudRechazadaRecientementeException extends RuntimeException {
    private final int diasRestantes;

    public SolicitudRechazadaRecientementeException(int diasRestantes) {
        super("Tu solicitud fue rechazada recientemente. Puedes intentarlo de nuevo en " + diasRestantes + " día(s).");
        this.diasRestantes = diasRestantes;
    }

    public int getDiasRestantes() {
        return diasRestantes;
    }
}
