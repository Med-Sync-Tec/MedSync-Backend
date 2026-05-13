package itesm.medsync.domain.specialty.exception;

/** Thrown when creating or updating a specialty would collide with an existing one. Maps to HTTP 409. */
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
