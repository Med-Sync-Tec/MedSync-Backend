package itesm.medsync.application.user;

import itesm.medsync.domain.specialty.usecase.GetSpecialtyByIdUseCase;
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

import java.util.UUID;

@ApplicationScoped
public class CreateAdminUserService implements CreateAdminUserUseCase {

    private static final Logger LOG = Logger.getLogger(CreateAdminUserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FirebaseUserGateway firebaseUserGateway;
    private final GetSpecialtyByIdUseCase getSpecialtyById;

    @Inject
    public CreateAdminUserService(UserRepository userRepository,
                                  RoleRepository roleRepository,
                                  FirebaseUserGateway firebaseUserGateway,
                                  GetSpecialtyByIdUseCase getSpecialtyById) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.firebaseUserGateway = firebaseUserGateway;
        this.getSpecialtyById = getSpecialtyById;
    }

    @Override
    @Transactional
    public UserWithRole execute(String email, String nombre, String password,
                                String roleName, UUID especialidadId) {
        Role role = roleRepository.findByNombre(roleName.toUpperCase())
                .orElseThrow(() -> new RoleNotFoundException(roleName));

        userRepository.findByEmail(email).ifPresent(existing -> {
            throw new UserAlreadyExistsException(email);
        });

        if (especialidadId != null) {
            getSpecialtyById.execute(especialidadId);   // 404s if missing
        }

        String firebaseUid = firebaseUserGateway.createUser(email, password, nombre);

        try {
            User newUser = User.create(nombre, email, especialidadId, role.getId());
            User saved = userRepository.save(newUser);
            return new UserWithRole(saved, role.getNombre());
        } catch (Exception e) {
            LOG.errorf("DB save failed after Firebase user creation (uid=%s); rolling back Firebase user", firebaseUid);
            firebaseUserGateway.deleteUser(firebaseUid);
            throw e;
        }
    }
}
