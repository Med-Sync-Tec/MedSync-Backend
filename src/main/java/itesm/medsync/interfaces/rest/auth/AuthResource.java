package itesm.medsync.interfaces.rest.auth;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.user.exception.RoleMismatchException;
import itesm.medsync.domain.user.model.KnownRoles;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.interfaces.rest.user.UserRestMapper;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "Login con verificación de rol esperado")
public class AuthResource {

    private static final String DEFAULT_EXPECTED_ROLE = KnownRoles.DOCTOR;

    private final AuthenticatedUserContext userContext;

    @Inject
    public AuthResource(AuthenticatedUserContext userContext) {
        this.userContext = userContext;
    }

    @POST
    @Path("/login")
    @Operation(summary = "Verifica que el usuario autenticado tenga el rol esperado")
    @APIResponse(responseCode = "200", description = "Login válido, devuelve usuario")
    @APIResponse(responseCode = "401", description = "No autenticado (sin token Firebase)")
    @APIResponse(responseCode = "403", description = "Rol del usuario no coincide con expectedRole")
    public Response login(@Valid LoginRequest request) {
        UserWithRole current = userContext.getCurrentUser();
        if (current == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String expected = (request != null && request.expectedRole != null && !request.expectedRole.isBlank())
                ? request.expectedRole.trim().toUpperCase()
                : DEFAULT_EXPECTED_ROLE;

        if (!current.roleName().equalsIgnoreCase(expected)) {
            throw new RoleMismatchException(current.roleName(), expected);
        }

        return Response.ok(UserRestMapper.toResponse(current.user(), current.roleName())).build();
    }
}
