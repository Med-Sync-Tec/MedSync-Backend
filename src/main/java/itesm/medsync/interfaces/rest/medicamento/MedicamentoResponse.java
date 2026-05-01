package itesm.medsync.interfaces.rest.medicamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record MedicamentoResponse(
        UUID id,
        String nombre,
        String estado,
        String descripcion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
