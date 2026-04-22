package itesm.medsync.domain.user.repository;

import itesm.medsync.domain.user.model.Role;

import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findByNombre(String nombre);
}
