package itesm.medsync.infrastructure.persistence.user;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class UserRepositoryImpl implements UserRepository, PanacheRepositoryBase<UserEntity, UUID> {

    @Override
    public Optional<User> findByEmail(String email) {
        return find("correo", email).firstResultOptional()
                .map(UserPersistenceMapper::toDomain);
    }

    @Override
    public User save(User user) {
        Optional<UserEntity> existing = findByIdOptional(user.getId());
        if (existing.isPresent()) {
            UserEntity managed = existing.get();
            UserPersistenceMapper.copyInto(user, managed);
            getEntityManager().flush();
            return UserPersistenceMapper.toDomain(managed);
        }
        UserEntity fresh = UserPersistenceMapper.toEntity(user);
        persist(fresh);
        flush();
        return UserPersistenceMapper.toDomain(fresh);
    }
}
