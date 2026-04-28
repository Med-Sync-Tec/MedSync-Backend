package itesm.medsync.domain.user.exception;

public class RoleMismatchException extends RuntimeException {

    private final String actualRole;
    private final String expectedRole;

    public RoleMismatchException(String actualRole, String expectedRole) {
        super("Role mismatch: cuenta tiene rol '" + actualRole
                + "', se intentó iniciar sesión como '" + expectedRole + "'");
        this.actualRole = actualRole;
        this.expectedRole = expectedRole;
    }

    public String getActualRole() {
        return actualRole;
    }

    public String getExpectedRole() {
        return expectedRole;
    }
}
