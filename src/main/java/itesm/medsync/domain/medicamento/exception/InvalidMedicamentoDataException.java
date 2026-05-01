package itesm.medsync.domain.medicamento.exception;

public class InvalidMedicamentoDataException extends RuntimeException {
    public InvalidMedicamentoDataException(String message) {
        super(message);
    }
}
