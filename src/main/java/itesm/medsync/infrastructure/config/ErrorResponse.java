package itesm.medsync.infrastructure.config;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldError> details
) {
    public ErrorResponse(int status, String error, String message) {
        this(Instant.now(), status, error, message, null);
    }

    public ErrorResponse(int status, String error, String message, List<FieldError> details) {
        this(Instant.now(), status, error, message, details);
    }

    public record FieldError(String field, String message) {
    }
}
