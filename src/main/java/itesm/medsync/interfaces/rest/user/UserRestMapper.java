package itesm.medsync.interfaces.rest.user;

import itesm.medsync.domain.user.model.User;

public final class UserRestMapper {

    private UserRestMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getNombre(),
                user.getCorreo(),
                user.getRolId(),
                user.isActivo(),
                user.getCreatedAt()
        );
    }
}
