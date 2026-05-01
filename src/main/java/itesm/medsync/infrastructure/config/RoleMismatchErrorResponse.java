package itesm.medsync.infrastructure.config;

import java.time.Instant;

public record RoleMismatchErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String expectedRole
) {
    public RoleMismatchErrorResponse(int status,
                                     String error,
                                     String message,
                                     String expectedRole) {
        this(Instant.now(), status, error, message, expectedRole);
    }
}
