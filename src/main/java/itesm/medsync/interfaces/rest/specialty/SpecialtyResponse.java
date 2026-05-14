package itesm.medsync.interfaces.rest.specialty;

import java.time.LocalDateTime;
import java.util.UUID;

/** Outbound DTO for a specialty. Never serializes the JPA entity directly. */
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
