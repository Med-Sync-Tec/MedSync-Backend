package itesm.medsync.domain.user.usecase;

import itesm.medsync.domain.user.model.UserWithRole;

public interface CreateAdminUserUseCase {
    // Password is supplied by the caller (COO admin flow) and passed directly to Firebase.
    UserWithRole execute(String email, String nombre, String password, String roleName);
}
