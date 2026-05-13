package itesm.medsync.domain.specialty.exception;

/** Thrown when a specialty field violates a domain invariant (length, format, blank). Maps to HTTP 400. */
public class InvalidSpecialtyDataException extends RuntimeException {

    public InvalidSpecialtyDataException(String message) {
        super(message);
    }
}
