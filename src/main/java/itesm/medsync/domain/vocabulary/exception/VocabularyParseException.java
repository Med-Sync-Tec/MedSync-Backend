package itesm.medsync.domain.vocabulary.exception;

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
