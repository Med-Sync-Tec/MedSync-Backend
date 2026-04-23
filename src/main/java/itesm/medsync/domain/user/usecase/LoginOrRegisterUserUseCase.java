package itesm.medsync.domain.user.usecase;

import itesm.medsync.domain.user.model.User;

public interface LoginOrRegisterUserUseCase {

    User execute(String email, String name);
}
