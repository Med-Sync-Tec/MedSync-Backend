package itesm.medsync.interfaces.rest.common;

import java.time.Instant;

public record RoleMismatchErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String actualRole,
        String expectedRole
) {
    public RoleMismatchErrorResponse(int status,
                                     String error,
                                     String message,
                                     String actualRole,
                                     String expectedRole) {
        this(Instant.now(), status, error, message, actualRole, expectedRole);
    }
}
