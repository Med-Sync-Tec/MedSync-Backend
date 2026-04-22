package itesm.medsync.infrastructure.persistence.user;

import itesm.medsync.domain.user.model.User;

public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    public static UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setNombre(user.getNombre());
        entity.setCorreo(user.getCorreo());
        entity.setRolId(user.getRolId());
        entity.setActivo(user.isActivo());
        entity.setCreatedAt(user.getCreatedAt());
        return entity;
    }

    public static void copyInto(User user, UserEntity entity) {
        entity.setNombre(user.getNombre());
        entity.setCorreo(user.getCorreo());
        entity.setRolId(user.getRolId());
        entity.setActivo(user.isActivo());
    }

    public static User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getNombre(),
                entity.getCorreo(),
                entity.getRolId(),
                entity.isActivo(),
                entity.getCreatedAt());
    }
}
