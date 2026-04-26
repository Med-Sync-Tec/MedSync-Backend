package itesm.medsync.infrastructure.hospital;

import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.model.ExpedienteClinico;

public final class HospitalPersistenceMapper {

    private HospitalPersistenceMapper() {
    }

    public static ExpedienteClinico toDomain(ExpedienteClinicoHospitalEntity entity) {
        return new ExpedienteClinico(
                entity.getId(),
                entity.getPacienteExternoId(),
                entity.getDoctorResponsableId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static ExpedienteClinicoHospitalEntity toEntity(ExpedienteClinico domain) {
        ExpedienteClinicoHospitalEntity entity = new ExpedienteClinicoHospitalEntity();
        entity.setId(domain.getId());
        entity.setPacienteExternoId(domain.getPacienteExternoId());
        entity.setDoctorResponsableId(domain.getDoctorResponsableId());
        return entity;
    }

    public static ConsultaHospitalEntity toEntity(Consulta domain) {
        ConsultaHospitalEntity entity = new ConsultaHospitalEntity();
        entity.setId(domain.getId());
        entity.setExpedienteId(domain.getExpedienteId());
        entity.setFecha(domain.getFecha());
        entity.setMotivoConsulta(domain.getMotivoConsulta());
        entity.setSubjetivo(domain.getSubjetivo());
        entity.setObjetivo(domain.getObjetivo());
        entity.setEvaluacion(domain.getEvaluacion());
        entity.setPlan(domain.getPlan());
        entity.setPrescripcion(domain.getPrescripcion());
        entity.setDiagnostico(domain.getDiagnostico());
        return entity;
    }

    public static Consulta toDomain(ConsultaHospitalEntity entity) {
        return new Consulta(
                entity.getId(),
                entity.getExpedienteId(),
                entity.getFecha(),
                entity.getMotivoConsulta(),
                entity.getSubjetivo(),
                entity.getObjetivo(),
                entity.getEvaluacion(),
                entity.getPlan(),
                entity.getPrescripcion(),
                entity.getDiagnostico(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
