package itesm.medsync.domain.user.repository;

import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.model.UserWithRole;

import java.util.Optional;

public interface UserRepository {

    Optional<UserWithRole> findByEmail(String email);

    User save(User user);
}
