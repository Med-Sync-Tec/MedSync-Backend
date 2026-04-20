package itesm.medsync.domain.hospital.exception;

import java.util.UUID;

public class ExpedienteNotFoundException extends RuntimeException {

    public ExpedienteNotFoundException(UUID patientId) {
        super("Expediente clínico not found for patient: " + patientId);
    }

    public ExpedienteNotFoundException(String pacienteExternoId) {
        super("Expediente clínico not found for pacienteExternoId: " + pacienteExternoId);
    }
}
