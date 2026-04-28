package itesm.medsync.interfaces.rest.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.pacientecontexto.usecase.AddPacienteContextoUseCase;
import itesm.medsync.domain.pacientecontexto.usecase.DeletePacienteContextoUseCase;
import itesm.medsync.domain.pacientecontexto.usecase.ListPacienteContextosByPacienteUseCase;
import itesm.medsync.domain.shared.model.TipoClinico;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
import java.util.List;
import java.util.UUID;

@Path("/api/patients/{patientId}/contextos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Paciente Contexto", description = "Tags clínicos normalizados por paciente")
public class PacienteContextoResource {

    private final AddPacienteContextoUseCase addContexto;
    private final ListPacienteContextosByPacienteUseCase listContextos;
    private final DeletePacienteContextoUseCase deleteContexto;

    @Inject
    public PacienteContextoResource(AddPacienteContextoUseCase addContexto,
                                    ListPacienteContextosByPacienteUseCase listContextos,
                                    DeletePacienteContextoUseCase deleteContexto) {
        this.addContexto = addContexto;
        this.listContextos = listContextos;
        this.deleteContexto = deleteContexto;
    }

    @POST
    @Operation(summary = "Agregar contexto clínico al paciente")
    @APIResponse(responseCode = "201", description = "Contexto creado")
    @APIResponse(responseCode = "400", description = "Datos inválidos")
    @APIResponse(responseCode = "404", description = "Paciente no encontrado")
    public Response add(@PathParam("patientId") UUID patientId,
                        @Valid AddPacienteContextoRequest request,
                        @Context UriInfo uriInfo) {
        TipoClinico tipo = TipoClinico.fromString(request.tipo);
        PacienteContexto created = addContexto.execute(patientId, tipo, request.valor);

        URI location = uriInfo.getAbsolutePathBuilder().path(created.getId().toString()).build();
        return Response.created(location)
                .entity(PacienteContextoRestMapper.toResponse(created))
                .build();
    }

    @GET
    @Operation(summary = "Listar contextos clínicos del paciente")
    @APIResponse(responseCode = "200", description = "Lista de contextos")
    @APIResponse(responseCode = "404", description = "Paciente no encontrado")
    public List<PacienteContextoResponse> listByPaciente(@PathParam("patientId") UUID patientId) {
        return listContextos.execute(patientId).stream()
                .map(PacienteContextoRestMapper::toResponse)
                .toList();
    }

    @DELETE
    @Path("/{contextoId}")
    @Operation(summary = "Eliminar un contexto clínico")
    @APIResponse(responseCode = "204", description = "Contexto eliminado")
    @APIResponse(responseCode = "404", description = "Contexto no encontrado")
    public Response delete(@PathParam("patientId") UUID patientId,
                           @PathParam("contextoId") UUID contextoId) {
        deleteContexto.execute(contextoId);
        return Response.noContent().build();
    }
}
