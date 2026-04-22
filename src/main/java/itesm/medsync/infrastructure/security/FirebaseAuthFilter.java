package itesm.medsync.infrastructure.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.usecase.LoginOrRegisterUserUseCase;
import itesm.medsync.infrastructure.config.ErrorResponse;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class FirebaseAuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(FirebaseAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final LoginOrRegisterUserUseCase loginOrRegister;
    private final AuthenticatedUserContext userContext;

    @Inject
    public FirebaseAuthFilter(LoginOrRegisterUserUseCase loginOrRegister,
                              AuthenticatedUserContext userContext) {
        this.loginOrRegister = loginOrRegister;
        this.userContext = userContext;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return;
        }

        String idToken = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (idToken.isEmpty()) {
            abortUnauthorized(requestContext, "Empty bearer token");
            return;
        }

        FirebaseToken decoded;
        try {
            decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException ex) {
            LOG.debugf("Invalid Firebase token: %s", ex.getMessage());
            abortUnauthorized(requestContext, "Invalid or expired Firebase token");
            return;
        } catch (IllegalStateException ex) {
            LOG.error("Firebase SDK not initialized; cannot verify authenticated request", ex);
            abortUnauthorized(requestContext, "Authentication service unavailable");
            return;
        }

        String email = decoded.getEmail();
        if (email == null || email.isBlank()) {
            LOG.warnf("Firebase token has no email claim (uid=%s); cannot sync user", decoded.getUid());
            abortUnauthorized(requestContext, "Token missing email claim");
            return;
        }

        String name = nameFromClaims(decoded, email);
        User user = loginOrRegister.execute(email, name);
        userContext.setCurrentUser(user);
    }

    private static String nameFromClaims(FirebaseToken decoded, String email) {
        Object raw = decoded.getClaims().get("name");
        if (raw instanceof String s && !s.isBlank()) {
            return s;
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private static void abortUnauthorized(ContainerRequestContext ctx, String detail) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse(401, "Unauthorized", detail))
                .build());
    }
}
