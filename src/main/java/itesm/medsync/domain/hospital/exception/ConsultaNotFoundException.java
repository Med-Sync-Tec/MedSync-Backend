package itesm.medsync.domain.hospital.exception;

public class ConsultaNotFoundException extends RuntimeException {

    public ConsultaNotFoundException(String consultaId) {
        super("Consulta not found: " + consultaId);
    }
}
