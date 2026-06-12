package itesm.medsync.infrastructure.security;

import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import itesm.medsync.domain.user.exception.UserAlreadyExistsException;
import itesm.medsync.domain.user.repository.FirebaseUserGateway;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class FirebaseUserGatewayImpl implements FirebaseUserGateway {

    private static final Logger LOG = Logger.getLogger(FirebaseUserGatewayImpl.class);

    @Override
    public String createUser(String email, String password, String displayName) {
        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisplayName(displayName);
            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
            return userRecord.getUid();
        } catch (FirebaseAuthException e) {
            if ("EMAIL_ALREADY_EXISTS".equals(e.getAuthErrorCode().name())) {
                throw new UserAlreadyExistsException(email);
            }
            LOG.errorf("Firebase user creation failed for %s: %s", email, e.getMessage());
            throw new IllegalStateException("Firebase user creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteUser(String uid) {
        try {
            FirebaseAuth.getInstance().deleteUser(uid);
        } catch (FirebaseAuthException e) {
            LOG.errorf("Failed to delete Firebase user %s: %s", uid, e.getMessage());
        }
    }

    @Override
    public String generatePasswordResetLink(String email, String continueUrl) {
        try {
            ActionCodeSettings settings = ActionCodeSettings.builder()
                    .setUrl(continueUrl)
                    .build();
            return FirebaseAuth.getInstance().generatePasswordResetLink(email, settings);
        } catch (FirebaseAuthException e) {
            LOG.errorf("Failed to generate password reset link for %s: %s", email, e.getMessage());
            throw new IllegalStateException("Could not generate password reset link: " + e.getMessage(), e);
        }
    }
}
