package itesm.medsync.interfaces.rest.hospital;

import java.time.LocalDateTime;

public record ExpedienteClinicoResponse(
        String id,
        String pacienteExternoId,
        String doctorResponsableId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
