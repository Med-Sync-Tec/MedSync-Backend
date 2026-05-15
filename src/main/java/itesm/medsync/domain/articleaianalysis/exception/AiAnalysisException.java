package itesm.medsync.domain.articleaianalysis.exception;

/**
 * Thrown when the upstream AI provider returns an unusable response —
 * non-2xx status, unparseable body, unknown specialty id, or zero valid tags
 * after the vocabulary filter. Maps to HTTP 502.
 */
public class AiAnalysisException extends RuntimeException {

    public AiAnalysisException(String message) {
        super(message);
    }

    public AiAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
