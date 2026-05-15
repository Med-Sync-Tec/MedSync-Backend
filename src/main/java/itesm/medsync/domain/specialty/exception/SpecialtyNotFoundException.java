package itesm.medsync.domain.specialty.exception;

import java.util.UUID;

/** Thrown when a specialty id does not resolve to a row (active or soft-deleted). Maps to HTTP 404. */
public class SpecialtyNotFoundException extends RuntimeException {

    public SpecialtyNotFoundException(UUID id) {
        super("Specialty not found: " + id);
    }
}
