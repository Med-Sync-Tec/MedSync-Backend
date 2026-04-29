package itesm.medsync.domain.medicamento.exception;

public class DuplicateMedicamentoException extends RuntimeException {
    public DuplicateMedicamentoException(String nombre) {
        super("Medicamento already exists: " + nombre);
    }
}
