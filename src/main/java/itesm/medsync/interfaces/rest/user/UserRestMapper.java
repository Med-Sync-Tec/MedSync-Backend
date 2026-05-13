package itesm.medsync.interfaces.rest.user;

import itesm.medsync.domain.user.model.User;

public final class UserRestMapper {

    private UserRestMapper() {
    }

    public static UserResponse toResponse(User user, String roleName) {
        return toResponse(user, roleName, null);
    }

    public static UserResponse toResponse(User user, String roleName, String especialidadNombre) {
        return new UserResponse(
                user.getId(),
                user.getNombre(),
                user.getCorreo(),
                roleName,
                user.getEspecialidadId(),
                especialidadNombre,
                user.isActivo(),
                user.getCreatedAt()
        );
    }
}
