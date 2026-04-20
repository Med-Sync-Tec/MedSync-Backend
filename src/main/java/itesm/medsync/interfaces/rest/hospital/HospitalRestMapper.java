package itesm.medsync.interfaces.rest.hospital;

import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.model.ExpedienteClinico;

import java.util.List;

public final class HospitalRestMapper {

    private HospitalRestMapper() {
    }

    public static ExpedienteClinicoResponse toResponse(ExpedienteClinico e) {
        return new ExpedienteClinicoResponse(
                e.getId(),
                e.getPacienteExternoId(),
                e.getDoctorResponsableId(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public static ConsultaResponse toResponse(Consulta c) {
        return new ConsultaResponse(
                c.getId(),
                c.getExpedienteId(),
                c.getFecha(),
                c.getMotivoConsulta(),
                c.getSubjetivo(),
                c.getObjetivo(),
                c.getEvaluacion(),
                c.getPlan(),
                c.getPrescripcion(),
                c.getDiagnostico(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }

    public static List<ConsultaResponse> toResponseList(List<Consulta> consultas) {
        return consultas.stream().map(HospitalRestMapper::toResponse).toList();
    }
}
