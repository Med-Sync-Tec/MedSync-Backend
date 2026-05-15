package itesm.medsync.application.user;

import itesm.medsync.domain.user.exception.RoleNotFoundException;
import itesm.medsync.domain.user.exception.UserAlreadyExistsException;
import itesm.medsync.domain.user.model.ProvisionApprovedUserResult;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.domain.user.repository.FirebaseUserGateway;
import itesm.medsync.domain.user.repository.RoleRepository;
import itesm.medsync.domain.user.repository.UserRepository;
import itesm.medsync.domain.user.usecase.ProvisionApprovedUserUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.security.SecureRandom;

@ApplicationScoped
public class ProvisionApprovedUserService implements ProvisionApprovedUserUseCase {

    private static final Logger LOG = Logger.getLogger(ProvisionApprovedUserService.class);

    // Characters used to generate the temporary Firebase password.
    private static final String PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final int PASSWORD_LENGTH = 24;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FirebaseUserGateway firebaseUserGateway;

    @ConfigProperty(name = "medsync.app.frontend-url")
    String frontendUrl;

    @Inject
    public ProvisionApprovedUserService(UserRepository userRepository,
                                        RoleRepository roleRepository,
                                        FirebaseUserGateway firebaseUserGateway) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.firebaseUserGateway = firebaseUserGateway;
    }

    @Override
    @Transactional
    public ProvisionApprovedUserResult execute(String email, String nombre, String roleName) {
        Role role = roleRepository.findByNombre(roleName.toUpperCase())
                .orElseThrow(() -> new RoleNotFoundException(roleName));

        userRepository.findByEmail(email).ifPresent(existing -> {
            throw new UserAlreadyExistsException(email);
        });

        // Generate a random password — it is passed to Firebase and immediately discarded.
        // The user never sees this password; they set their own via the reset link below.
        String tempPassword = generateSecurePassword();
        String firebaseUid = firebaseUserGateway.createUser(email, tempPassword, nombre);

        try {
            User newUser = User.create(nombre, email, null, role.getId());
            User saved = userRepository.save(newUser);
            UserWithRole created = new UserWithRole(saved, role.getNombre());

            // After password is set, Firebase redirects to the login page.
            String resetLink = firebaseUserGateway.generatePasswordResetLink(email, frontendUrl);

            return new ProvisionApprovedUserResult(created, resetLink);
        } catch (Exception e) {
            LOG.errorf("DB save failed after Firebase user creation (uid=%s); rolling back", firebaseUid);
            firebaseUserGateway.deleteUser(firebaseUid);
            throw e;
        }
    }

    private static String generateSecurePassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
