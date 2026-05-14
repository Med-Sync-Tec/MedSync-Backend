package itesm.medsync.application.user;

import itesm.medsync.domain.user.exception.RoleNotFoundException;
import itesm.medsync.domain.user.exception.UserAlreadyExistsException;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.domain.user.repository.FirebaseUserGateway;
import itesm.medsync.domain.user.repository.RoleRepository;
import itesm.medsync.domain.user.repository.UserRepository;
import itesm.medsync.domain.user.usecase.CreateAdminUserUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CreateAdminUserService implements CreateAdminUserUseCase {

    private static final Logger LOG = Logger.getLogger(CreateAdminUserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FirebaseUserGateway firebaseUserGateway;

    @Inject
    public CreateAdminUserService(UserRepository userRepository,
                                  RoleRepository roleRepository,
                                  FirebaseUserGateway firebaseUserGateway) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.firebaseUserGateway = firebaseUserGateway;
    }

    @Override
    @Transactional
    public UserWithRole execute(String email, String nombre, String password, String roleName) {
        Role role = roleRepository.findByNombre(roleName.toUpperCase())
                .orElseThrow(() -> new RoleNotFoundException(roleName));

        userRepository.findByEmail(email).ifPresent(existing -> {
            throw new UserAlreadyExistsException(email);
        });

        // Password is supplied by the COO — passed directly to Firebase, never stored here.
        String firebaseUid = firebaseUserGateway.createUser(email, password, nombre);

        try {
            User newUser = User.create(nombre, email, null, role.getId());
            User saved = userRepository.save(newUser);
            return new UserWithRole(saved, role.getNombre());
        } catch (Exception e) {
            LOG.errorf("DB save failed after Firebase user creation (uid=%s); rolling back", firebaseUid);
            firebaseUserGateway.deleteUser(firebaseUid);
            throw e;
        }
    }
}
