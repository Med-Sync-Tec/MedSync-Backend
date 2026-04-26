package itesm.medsync.domain.patient.exception;

public class DuplicatePatientException extends RuntimeException {

    private final String expedienteExternoId;

    public DuplicatePatientException(String expedienteExternoId) {
        super("Patient already exists with expediente: " + expedienteExternoId);
        this.expedienteExternoId = expedienteExternoId;
    }

    public String getExpedienteExternoId() {
        return expedienteExternoId;
    }
}
