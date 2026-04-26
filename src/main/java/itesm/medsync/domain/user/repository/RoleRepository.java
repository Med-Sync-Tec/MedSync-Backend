package itesm.medsync.domain.user.repository;

import itesm.medsync.domain.user.model.Role;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {

    Optional<Role> findByNombre(String nombre);

    Optional<Role> findByUuid(UUID id);
}
