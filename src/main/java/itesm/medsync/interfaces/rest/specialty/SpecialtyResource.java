package itesm.medsync.interfaces.rest.specialty;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.usecase.GetSpecialtyByIdUseCase;
import itesm.medsync.domain.specialty.usecase.ListActiveSpecialtiesUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/especialidades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Specialties", description = "Catálogo de especialidades médicas")
public class SpecialtyResource {

    private final ListActiveSpecialtiesUseCase listActive;
    private final GetSpecialtyByIdUseCase getById;
    private final AuthenticatedUserContext userContext;

    @Inject
    public SpecialtyResource(ListActiveSpecialtiesUseCase listActive,
                             GetSpecialtyByIdUseCase getById,
                             AuthenticatedUserContext userContext) {
        this.listActive = listActive;
        this.getById = getById;
        this.userContext = userContext;
    }

    @GET
    @Operation(summary = "Listar especialidades activas, ordenadas alfabéticamente")
    @APIResponse(responseCode = "200")
    @APIResponse(responseCode = "401", description = "No autenticado")
    public Response listAll() {
        if (userContext.getCurrentUser() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        List<SpecialtyResponse> result = SpecialtyRestMapper.toResponseList(listActive.execute());
        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Obtener especialidad por id")
    @APIResponse(responseCode = "200")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "404", description = "Especialidad no encontrada")
    public Response getOne(@PathParam("id") UUID id) {
        if (userContext.getCurrentUser() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Specialty s = getById.execute(id);
        return Response.ok(SpecialtyRestMapper.toResponse(s)).build();
    }
}
