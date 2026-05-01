package itesm.medsync.domain.user.usecase;

import itesm.medsync.domain.user.model.UserWithRole;

public interface CreateAdminUserUseCase {
    UserWithRole execute(String email, String nombre, String password, String roleName);
}
