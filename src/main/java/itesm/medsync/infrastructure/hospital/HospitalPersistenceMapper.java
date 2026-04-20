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
