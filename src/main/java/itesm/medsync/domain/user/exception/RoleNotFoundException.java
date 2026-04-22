package itesm.medsync.domain.user.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String nombre) {
        super("Role not found: " + nombre);
    }
}
