package itesm.medsync.interfaces.rest.pacientecontexto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PacienteContextoResponse(
        UUID id,
        UUID pacienteId,
        String tipo,
        String valor,
        LocalDateTime createdAt
) {
}
