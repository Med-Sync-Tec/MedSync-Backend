package itesm.medsync.interfaces.rest.admin;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.usecase.CreateSpecialtyUseCase;
import itesm.medsync.domain.specialty.usecase.SoftDeleteSpecialtyUseCase;
import itesm.medsync.domain.specialty.usecase.UpdateSpecialtyUseCase;
import itesm.medsync.domain.user.model.KnownRoles;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.interfaces.rest.common.ErrorResponse;
import itesm.medsync.interfaces.rest.specialty.CreateSpecialtyRequest;
import itesm.medsync.interfaces.rest.specialty.SpecialtyRestMapper;
import itesm.medsync.interfaces.rest.specialty.UpdateSpecialtyRequest;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.UUID;

/**
 * Write-only REST surface for the specialty catalog (COO-only).
 *
 * Authentication is enforced per-method via {@link #enforceCoo()} — the request
 * is rejected with 401 if no authenticated user is present and 403 if the user
 * is authenticated but not the COO. Read operations live in {@code SpecialtyResource}.
 */
@Path("/api/admin/especialidades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "Operaciones administrativas")
public class SpecialtyAdminResource {

    private final CreateSpecialtyUseCase createSpecialty;
    private final UpdateSpecialtyUseCase updateSpecialty;
    private final SoftDeleteSpecialtyUseCase softDeleteSpecialty;
    private final AuthenticatedUserContext userContext;

    @Inject
    public SpecialtyAdminResource(CreateSpecialtyUseCase createSpecialty,
                                  UpdateSpecialtyUseCase updateSpecialty,
                                  SoftDeleteSpecialtyUseCase softDeleteSpecialty,
                                  AuthenticatedUserContext userContext) {
        this.createSpecialty = createSpecialty;
        this.updateSpecialty = updateSpecialty;
        this.softDeleteSpecialty = softDeleteSpecialty;
        this.userContext = userContext;
    }

    @POST
    @Operation(summary = "Crear una especialidad (COO)")
    @APIResponse(responseCode = "201", description = "Especialidad creada")
    @APIResponse(responseCode = "400", description = "Datos inválidos")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "Solo el COO puede administrar el catálogo")
    @APIResponse(responseCode = "409", description = "nombre o slug duplicado")
    public Response create(@Valid CreateSpecialtyRequest request, @Context UriInfo uriInfo) {
        Response auth = enforceCoo();
        if (auth != null) return auth;

        Specialty created = createSpecialty.execute(request.nombre, request.slug, request.descripcion);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getId().toString()).build();
        return Response.created(location)
                .entity(SpecialtyRestMapper.toResponse(created))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Actualizar una especialidad (COO)")
    @APIResponse(responseCode = "200")
    @APIResponse(responseCode = "400", description = "Datos inválidos")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "Solo el COO puede administrar el catálogo")
    @APIResponse(responseCode = "404", description = "Especialidad no encontrada")
    @APIResponse(responseCode = "409", description = "nombre o slug duplicado")
    public Response update(@PathParam("id") UUID id, @Valid UpdateSpecialtyRequest request) {
        Response auth = enforceCoo();
        if (auth != null) return auth;

        Specialty updated = updateSpecialty.execute(id, request.nombre, request.slug, request.descripcion);
        return Response.ok(SpecialtyRestMapper.toResponse(updated)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Soft-delete de una especialidad (COO)")
    @APIResponse(responseCode = "204")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "Solo el COO puede administrar el catálogo")
    @APIResponse(responseCode = "404", description = "Especialidad no encontrada")
    public Response delete(@PathParam("id") UUID id) {
        Response auth = enforceCoo();
        if (auth != null) return auth;

        softDeleteSpecialty.execute(id);
        return Response.noContent().build();
    }

    /**
     * Returns {@code null} when the caller is authorized; otherwise returns the
     * pre-built 401 or 403 response the handler should send back.
     *
     * Pattern is duplicated across admin resources rather than extracted into a
     * filter because role checks here are simple and per-endpoint visibility is
     * easier to audit than a centralized filter.
     */
    private Response enforceCoo() {
        UserWithRole caller = userContext.getCurrentUser();
        if (caller == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (!KnownRoles.COO.equalsIgnoreCase(caller.roleName())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse(403, "Forbidden",
                            "Solo el COO puede administrar el catálogo de especialidades"))
                    .build();
        }
        return null;
    }
}
