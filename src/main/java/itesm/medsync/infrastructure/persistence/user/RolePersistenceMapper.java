package itesm.medsync.infrastructure.persistence.user;

import itesm.medsync.domain.user.model.Role;

public final class RolePersistenceMapper {

    private RolePersistenceMapper() {
    }

    public static Role toDomain(RoleEntity entity) {
        return new Role(
                entity.getId(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getCreatedAt());
    }
}
