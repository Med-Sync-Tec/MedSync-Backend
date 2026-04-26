package itesm.medsync.interfaces.rest.hospital;

import java.time.LocalDateTime;

public record ConsultaResponse(
        String id,
        String expedienteId,
        LocalDateTime fecha,
        String motivoConsulta,
        String subjetivo,
        String objetivo,
        String evaluacion,
        String plan,
        String prescripcion,
        String diagnostico,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
