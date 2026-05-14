package itesm.medsync.domain.articleaianalysis.exception;

/**
 * Thrown when an AI provider call exceeds the configured {@code ai.groq.timeout}.
 * Distinct from {@link AiAnalysisException} so monitoring can alert on timeouts
 * separately. Maps to HTTP 504.
 */
public class AiAnalysisTimeoutException extends RuntimeException {

    public AiAnalysisTimeoutException(String message) {
        super(message);
    }

    public AiAnalysisTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
