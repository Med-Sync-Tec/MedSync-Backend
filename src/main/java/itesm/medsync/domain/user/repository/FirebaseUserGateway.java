package itesm.medsync.domain.user.repository;

public interface FirebaseUserGateway {
    String createUser(String email, String password, String displayName);
    void deleteUser(String uid);
    // continueUrl: where Firebase redirects the user after the password is set
    String generatePasswordResetLink(String email, String continueUrl);
}
