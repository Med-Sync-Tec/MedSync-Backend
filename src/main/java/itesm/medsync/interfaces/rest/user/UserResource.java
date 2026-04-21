package itesm.medsync.interfaces.rest.user;

import itesm.medsync.application.security.AuthenticatedUserContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class UserResource {

    @Inject
    AuthenticatedUserContext userContext;

    @GET
    @Path("/me") // URL final: /api/users/me
    public Response getCurrentUser() {
        var user = userContext.getCurrentUser();
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(user).build();
    }
}