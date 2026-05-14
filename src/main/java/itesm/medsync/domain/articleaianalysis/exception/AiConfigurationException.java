package itesm.medsync.domain.articleaianalysis.exception;

/**
 * Thrown at application startup when {@code ai.groq.api-key} is missing or blank.
 *
 * Fires from the gateway's {@code @Startup} observer, aborting boot before HTTP
 * traffic starts. Intentionally NOT wired into {@code GlobalExceptionHandler} —
 * by the time HTTP is up, configuration has already been validated.
 */
public class AiConfigurationException extends RuntimeException {

    public AiConfigurationException(String message) {
        super(message);
    }
}
