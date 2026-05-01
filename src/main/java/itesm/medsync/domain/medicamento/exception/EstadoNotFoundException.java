package itesm.medsync.domain.medicamento.exception;

public class EstadoNotFoundException extends RuntimeException {
    public EstadoNotFoundException(String nombre) {
        super("Estado not found: " + nombre);
    }
}
