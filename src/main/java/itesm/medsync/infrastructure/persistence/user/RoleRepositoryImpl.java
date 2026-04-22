package itesm.medsync.infrastructure.persistence.user;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.repository.RoleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class RoleRepositoryImpl implements RoleRepository, PanacheRepositoryBase<RoleEntity, UUID> {

    @Override
    public Optional<Role> findByNombre(String nombre) {
        return find("nombre", nombre).firstResultOptional()
                .map(RolePersistenceMapper::toDomain);
    }
}
