package itesm.medsync.domain.medicamento.exception;

import java.util.UUID;

public class MedicamentoNotFoundException extends RuntimeException {
    public MedicamentoNotFoundException(UUID id) {
        super("Medicamento not found: " + id);
    }
}
