package itesm.medsync.interfaces.rest.patient;

import itesm.medsync.domain.patient.model.Patient;

public final class PatientRestMapper {

    private PatientRestMapper() {
    }

    public static PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getExpedienteExternoId(),
                patient.getNombre(),
                patient.getFechaNacimiento(),
                patient.getGenero(),
                patient.getMedicoId(),
                patient.isActivo(),
                patient.getCreatedAt(),
                patient.getUpdatedAt()
        );
    }
}
