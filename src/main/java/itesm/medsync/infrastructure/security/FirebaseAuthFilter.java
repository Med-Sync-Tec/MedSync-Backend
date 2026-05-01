package itesm.medsync.infrastructure.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.domain.user.usecase.LoginOrRegisterUserUseCase;
import itesm.medsync.interfaces.rest.common.ErrorResponse;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class FirebaseAuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(FirebaseAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    // Prefijos de paths que NO requieren autenticación (health, métricas, OpenAPI/Swagger).
    // Todo lo que esté bajo /api/** queda protegido.
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of("q/");

    private final LoginOrRegisterUserUseCase loginOrRegister;
    private final AuthenticatedUserContext userContext;

    @ConfigProperty(name = "medsync.security.firebase-filter.enabled", defaultValue = "true")
    boolean enabled;

    @Inject
    public FirebaseAuthFilter(LoginOrRegisterUserUseCase loginOrRegister,
                              AuthenticatedUserContext userContext) {
        this.loginOrRegister = loginOrRegister;
        this.userContext = userContext;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!enabled) {
            // Permite que tests con @InjectMock AuthenticatedUserContext controlen el usuario actual
            // sin tener que adjuntar tokens reales de Firebase. Producción y dev mantienen enabled=true.
            return;
        }
        if (isPublicRequest(requestContext)) {
            return;
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            abortUnauthorized(requestContext, "Authentication required");
            return;
        }
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            abortUnauthorized(requestContext, "Authorization header must use Bearer scheme");
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
        UserWithRole user = loginOrRegister.execute(email, name);
        userContext.setCurrentUser(user);
    }

    private static boolean isPublicRequest(ContainerRequestContext ctx) {
        // Permitir CORS preflight sin autenticación
        if (HttpMethod.OPTIONS.equalsIgnoreCase(ctx.getMethod())) {
            return true;
        }
        String path = ctx.getUriInfo().getPath();
        for (String prefix : PUBLIC_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
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
