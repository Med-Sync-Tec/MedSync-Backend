package itesm.medsync.interfaces.rest.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String nombre,
        String correo,
        String role,
        UUID especialidadId,
        String especialidadNombre,
        boolean activo,
        LocalDateTime createdAt
) {
}
