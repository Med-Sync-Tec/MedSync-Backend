package itesm.medsync.domain.specialty.exception;

import java.util.UUID;

public class SpecialtyNotFoundException extends RuntimeException {

    public SpecialtyNotFoundException(UUID id) {
        super("Specialty not found: " + id);
    }
}
