package itesm.medsync.domain.vocabulary.exception;

/**
 * Thrown by the JSON loader when a vocabulary file violates the schema (missing
 * field, slug mismatch, unknown TipoClinico key, duplicate term, malformed JSON).
 *
 * Fires exclusively at application startup, so it is intentionally NOT wired into
 * {@code GlobalExceptionHandler} — by the time HTTP is up, parsing has finished.
 * Letting the exception propagate out of the startup observer aborts the boot,
 * which is the desired behavior: a typo in clinical vocabulary should never
 * silently degrade AI extraction in production.
 */
public class VocabularyParseException extends RuntimeException {

    private final String filename;
    private final String offendingField;

    public VocabularyParseException(String filename, String offendingField, String message) {
        super(buildMessage(filename, offendingField, message));
        this.filename = filename;
        this.offendingField = offendingField;
    }

    public VocabularyParseException(String filename, String offendingField, String message, Throwable cause) {
        super(buildMessage(filename, offendingField, message), cause);
        this.filename = filename;
        this.offendingField = offendingField;
    }

    private static String buildMessage(String filename, String offendingField, String message) {
        StringBuilder sb = new StringBuilder("Vocabulary parse error in '").append(filename).append("'");
        if (offendingField != null && !offendingField.isBlank()) {
            sb.append(" [").append(offendingField).append("]");
        }
        sb.append(": ").append(message);
        return sb.toString();
    }

    public String getFilename() {
        return filename;
    }

    public String getOffendingField() {
        return offendingField;
    }
}
