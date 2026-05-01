package itesm.medsync.domain.user.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String correo) {
        super("User already exists: " + correo);
    }
}
