package itesm.medsync.infrastructure.persistence.user;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.domain.user.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityGraph;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class UserRepositoryImpl implements UserRepository, PanacheRepositoryBase<UserEntity, UUID> {

    @Override
    public Optional<UserWithRole> findByEmail(String email) {
        EntityGraph<?> graph = getEntityManager().getEntityGraph("User.withRole");
        return getEntityManager()
                .createQuery("SELECT u FROM UserEntity u WHERE u.correo = :email", UserEntity.class)
                .setParameter("email", email)
                .setHint("jakarta.persistence.fetchgraph", graph)
                .getResultStream()
                .findFirst()
                .map(UserPersistenceMapper::toDomainWithRole);
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
