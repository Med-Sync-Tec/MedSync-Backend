package itesm.medsync.domain.user.repository;

public interface FirebaseUserGateway {
    String createUser(String email, String password, String displayName);
    void deleteUser(String uid);
}
