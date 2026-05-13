package itesm.medsync.domain.user.usecase;

import itesm.medsync.domain.user.model.UserWithRole;

import java.util.UUID;

public interface CreateAdminUserUseCase {

    UserWithRole execute(String email, String nombre, String password, String roleName, UUID especialidadId);
}
