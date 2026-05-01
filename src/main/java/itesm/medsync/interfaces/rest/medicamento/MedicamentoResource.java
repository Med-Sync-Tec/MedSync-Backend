package itesm.medsync.interfaces.rest.medicamento;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.medicamento.usecase.CreateMedicamentoUseCase;
import itesm.medsync.domain.medicamento.usecase.DeleteMedicamentoUseCase;
import itesm.medsync.domain.medicamento.usecase.GetMedicamentoByIdUseCase;
import itesm.medsync.domain.medicamento.usecase.ListMedicamentosUseCase;
import itesm.medsync.domain.medicamento.usecase.UpdateMedicamentoEstadoUseCase;
import itesm.medsync.domain.medicamento.usecase.UpdateMedicamentoUseCase;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/medicamentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Medicamentos", description = "Gestión de inventario de medicamentos")
public class MedicamentoResource {

    private final ListMedicamentosUseCase listMedicamentos;
    private final GetMedicamentoByIdUseCase getMedicamentoById;
    private final CreateMedicamentoUseCase createMedicamento;
    private final UpdateMedicamentoUseCase updateMedicamento;
    private final UpdateMedicamentoEstadoUseCase updateEstado;
    private final DeleteMedicamentoUseCase deleteMedicamento;
    private final AuthenticatedUserContext userContext;

    @Inject
    public MedicamentoResource(ListMedicamentosUseCase listMedicamentos,
                                GetMedicamentoByIdUseCase getMedicamentoById,
                                CreateMedicamentoUseCase createMedicamento,
                                UpdateMedicamentoUseCase updateMedicamento,
                                UpdateMedicamentoEstadoUseCase updateEstado,
                                DeleteMedicamentoUseCase deleteMedicamento,
                                AuthenticatedUserContext userContext) {
        this.listMedicamentos = listMedicamentos;
        this.getMedicamentoById = getMedicamentoById;
        this.createMedicamento = createMedicamento;
        this.updateMedicamento = updateMedicamento;
        this.updateEstado = updateEstado;
        this.deleteMedicamento = deleteMedicamento;
        this.userContext = userContext;
    }

    private void requireAuthenticated() {
        if (userContext.getCurrentUser() == null) {
            throw new WebApplicationException("Authentication required", Response.Status.UNAUTHORIZED);
        }
    }

    @GET
    @Operation(summary = "Listar medicamentos paginados")
    @APIResponse(responseCode = "200", description = "Página de medicamentos")
    @APIResponse(responseCode = "401", description = "No autenticado")
    public MedicamentosPageResponse listAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("nombre") String nombre,
            @QueryParam("estado") String estado) {
        requireAuthenticated();
        var result = listMedicamentos.execute(nombre, estado, page, size);
        List<MedicamentoResponse> content = result.content().stream()
                .map(MedicamentoRestMapper::toResponse)
                .toList();
        return new MedicamentosPageResponse(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Obtener medicamento por ID")
    @APIResponse(responseCode = "200", description = "Medicamento encontrado")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "404", description = "Medicamento no encontrado")
    public MedicamentoResponse getById(@PathParam("id") UUID id) {
        requireAuthenticated();
        return MedicamentoRestMapper.toResponse(getMedicamentoById.execute(id));
    }

    @POST
    @Operation(summary = "Crear medicamento")
    @APIResponse(responseCode = "201", description = "Medicamento creado")
    @APIResponse(responseCode = "400", description = "Datos inválidos")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "409", description = "Nombre ya existe")
    public Response create(@Valid CreateMedicamentoRequest request, @Context UriInfo uriInfo) {
        requireAuthenticated();
        var created = createMedicamento.execute(request.nombre, request.descripcion);
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.medicamento().getId().toString())
                .build();
        return Response.created(location)
                .entity(MedicamentoRestMapper.toResponse(created))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Actualizar medicamento")
    @APIResponse(responseCode = "200", description = "Medicamento actualizado")
    @APIResponse(responseCode = "400", description = "Datos inválidos")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "404", description = "Medicamento no encontrado")
    @APIResponse(responseCode = "409", description = "Nombre ya existe")
    public MedicamentoResponse update(@PathParam("id") UUID id,
                                      @Valid UpdateMedicamentoRequest request) {
        requireAuthenticated();
        return MedicamentoRestMapper.toResponse(
                updateMedicamento.execute(id, request.nombre, request.estado, request.descripcion));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar medicamento")
    @APIResponse(responseCode = "204", description = "Medicamento eliminado")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "404", description = "Medicamento no encontrado")
    public Response delete(@PathParam("id") UUID id) {
        requireAuthenticated();
        deleteMedicamento.execute(id);
        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id}/estado")
    @Operation(summary = "Actualizar estado del medicamento")
    @APIResponse(responseCode = "200", description = "Estado actualizado")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "404", description = "Medicamento o estado no encontrado")
    public MedicamentoResponse updateEstado(@PathParam("id") UUID id,
                                            @Valid UpdateEstadoRequest request) {
        requireAuthenticated();
        return MedicamentoRestMapper.toResponse(updateEstado.execute(id, request.estado));
    }
}
