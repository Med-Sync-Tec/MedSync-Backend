package itesm.medsync.infrastructure.persistence.patient;

import itesm.medsync.domain.patient.model.Patient;

public final class PatientPersistenceMapper {

    private PatientPersistenceMapper() {
    }

    public static PatientEntity toEntity(Patient patient) {
        PatientEntity entity = new PatientEntity();
        entity.setId(patient.getId());
        entity.setExpedienteExternoId(patient.getExpedienteExternoId());
        entity.setNombre(patient.getNombre());
        entity.setFechaNacimiento(patient.getFechaNacimiento());
        entity.setGenero(patient.getGenero());
        entity.setMedicoId(patient.getMedicoId());
        entity.setActivo(patient.isActivo());
        entity.setCreatedAt(patient.getCreatedAt());
        entity.setUpdatedAt(patient.getUpdatedAt());
        return entity;
    }

    public static void copyInto(Patient patient, PatientEntity entity) {
        entity.setExpedienteExternoId(patient.getExpedienteExternoId());
        entity.setNombre(patient.getNombre());
        entity.setFechaNacimiento(patient.getFechaNacimiento());
        entity.setGenero(patient.getGenero());
        entity.setMedicoId(patient.getMedicoId());
        entity.setActivo(patient.isActivo());
    }

    public static Patient toDomain(PatientEntity entity) {
        return new Patient(
                entity.getId(),
                entity.getExpedienteExternoId(),
                entity.getNombre(),
                entity.getFechaNacimiento(),
                entity.getGenero(),
                entity.getMedicoId(),
                entity.isActivo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
