package itesm.medsync.domain.consultaaianalysis.exception;

/**
 * Thrown when an authenticated user invokes the consulta-analysis flow but
 * has no {@code especialidadId} set. The vocabulary cannot be chosen without
 * a specialty, so we refuse the call rather than guess. Maps to HTTP 400.
 */
public class UserHasNoSpecialtyException extends RuntimeException {

    public UserHasNoSpecialtyException(String message) {
        super(message);
    }
}
