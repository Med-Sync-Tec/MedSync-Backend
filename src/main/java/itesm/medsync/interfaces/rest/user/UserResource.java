package itesm.medsync.interfaces.rest.user;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.user.model.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "Usuarios autenticados")
public class UserResource {

    private final AuthenticatedUserContext userContext;

    @Inject
    public UserResource(AuthenticatedUserContext userContext) {
        this.userContext = userContext;
    }

    @GET
    @Path("/me")
    @Operation(summary = "Obtener el usuario autenticado")
    @APIResponse(responseCode = "200", description = "Usuario autenticado")
    @APIResponse(responseCode = "401", description = "No autenticado")
    public Response getCurrentUser() {
        User user = userContext.getCurrentUser();
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(UserRestMapper.toResponse(user)).build();
    }
}
