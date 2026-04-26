package itesm.medsync.application.user;

import itesm.medsync.domain.user.exception.RoleNotFoundException;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.domain.user.repository.RoleRepository;
import itesm.medsync.domain.user.repository.UserRepository;
import itesm.medsync.domain.user.usecase.LoginOrRegisterUserUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RegisterUserService implements LoginOrRegisterUserUseCase {

    private static final String DEFAULT_ROLE = "DOCTOR";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Inject
    public RegisterUserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public UserWithRole execute(String email, String name) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> createDefaultUser(email, name));
    }

    private UserWithRole createDefaultUser(String email, String name) {
        Role role = roleRepository.findByNombre(DEFAULT_ROLE)
                .orElseThrow(() -> new RoleNotFoundException(DEFAULT_ROLE));
        User newUser = User.create(name, email, role.getId());
        User saved = userRepository.save(newUser);
        return new UserWithRole(saved, role.getNombre());
    }
}
