package itesm.medsync.domain.user.usecase;

import itesm.medsync.domain.user.model.UserWithRole;

public interface LoginOrRegisterUserUseCase {

    UserWithRole execute(String email, String name);
}
