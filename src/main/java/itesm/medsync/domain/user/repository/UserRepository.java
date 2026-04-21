package itesm.medsync.domain.user.repository;

import itesm.medsync.domain.user.model.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);

    User save(User user);
}