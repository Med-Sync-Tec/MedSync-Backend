package itesm.medsync.infrastructure.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.application.user.RegisterUserService;
import itesm.medsync.domain.user.model.User;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class FirebaseAuthFilter implements ContainerRequestFilter {

    @Inject
    RegisterUserService registerUserService;

    @Inject
    AuthenticatedUserContext userContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString("Authorization");

        // 1. Verificar si el encabezado existe y tiene el formato Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String idToken = authHeader.substring(7);
        try {
            // 2. Validar el token con Firebase
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String email = decodedToken.getEmail();

            // 3. Manejar el caso donde el nombre no viene en el token (importante para
            // evitar errores en DB)
            String name = (String) decodedToken.getClaims().getOrDefault("name", email.split("@")[0]);

            // 4. Sincronizar con nuestra base de datos local
            User user = registerUserService.loginOrRegister(email, name);

            // 5. Guardar el usuario en el contexto de la petición para que UserResource
            // pueda leerlo
            userContext.setCurrentUser(user);

        } catch (Exception e) {
            // Imprimimos el error real en la terminal para debuggear (Firebase mismatch, DB
            // error, etc)
            System.err.println("Error en FirebaseAuthFilter: " + e.getMessage());
            e.printStackTrace();
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}