package itesm.medsync.interfaces.rest.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String nombre,
        String correo,
        String role,
        boolean activo,
        LocalDateTime createdAt
) {
}
