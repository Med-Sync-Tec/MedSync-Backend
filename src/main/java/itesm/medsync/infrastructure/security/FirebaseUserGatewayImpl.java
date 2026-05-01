package itesm.medsync.infrastructure.security;

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
            UserRecord record = FirebaseAuth.getInstance().createUser(request);
            return record.getUid();
        } catch (FirebaseAuthException e) {
            if ("EMAIL_ALREADY_EXISTS".equals(e.getAuthErrorCode().name())) {
                throw new UserAlreadyExistsException(email);
            }
            LOG.errorf("Firebase user creation failed for %s: %s", email, e.getMessage());
            throw new RuntimeException("Firebase user creation failed: " + e.getMessage(), e);
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
}
