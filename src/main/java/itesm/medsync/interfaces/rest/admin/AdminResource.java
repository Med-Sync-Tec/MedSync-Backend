package itesm.medsync.interfaces.rest.admin;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.user.model.KnownRoles;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.domain.user.usecase.CreateAdminUserUseCase;
import itesm.medsync.interfaces.rest.common.ErrorResponse;
import itesm.medsync.interfaces.rest.user.UserRestMapper;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;

@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "Operaciones administrativas")
public class AdminResource {

    private final CreateAdminUserUseCase createAdminUser;
    private final AuthenticatedUserContext userContext;

    @Inject
    public AdminResource(CreateAdminUserUseCase createAdminUser,
                         AuthenticatedUserContext userContext) {
        this.createAdminUser = createAdminUser;
        this.userContext = userContext;
    }

    @POST
    @Path("/users")
    @Operation(summary = "Crear un nuevo usuario con rol específico")
    @APIResponse(responseCode = "201", description = "Usuario creado")
    @APIResponse(responseCode = "400", description = "Datos inválidos")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "Solo el COO puede crear usuarios")
    @APIResponse(responseCode = "409", description = "El correo ya está registrado")
    public Response createUser(@Valid CreateUserRequest request, @Context UriInfo uriInfo) {
        UserWithRole caller = userContext.getCurrentUser();
        if (caller == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (!KnownRoles.COO.equalsIgnoreCase(caller.roleName())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse(403, "Forbidden", "Solo el COO puede crear usuarios"))
                    .build();
        }

        UserWithRole created = createAdminUser.execute(
                request.correo,
                request.nombre,
                request.password,
                request.rol,
                request.especialidadId
        );

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.user().getId().toString())
                .build();

        return Response.created(location)
                .entity(UserRestMapper.toResponse(created.user(), created.roleName()))
                .build();
    }
}
