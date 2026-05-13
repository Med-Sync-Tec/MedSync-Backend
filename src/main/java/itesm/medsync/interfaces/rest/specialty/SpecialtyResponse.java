package itesm.medsync.interfaces.rest.specialty;

import java.time.LocalDateTime;
import java.util.UUID;

public record SpecialtyResponse(
        UUID id,
        String nombre,
        String slug,
        String descripcion,
        boolean activo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
