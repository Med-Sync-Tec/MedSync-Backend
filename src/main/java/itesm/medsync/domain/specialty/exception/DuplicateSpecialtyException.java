package itesm.medsync.domain.specialty.exception;

public class DuplicateSpecialtyException extends RuntimeException {

    private final String field;
    private final String value;

    public DuplicateSpecialtyException(String field, String value) {
        super("Specialty already exists with " + field + ": " + value);
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
