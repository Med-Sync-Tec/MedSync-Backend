package itesm.medsync.interfaces.rest.hospital;

import itesm.medsync.domain.hospital.usecase.GetConsultaByIdUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/consultas")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Hospital", description = "Integración read-only con el sistema del hospital externo")
public class HospitalConsultaResource {

    private final GetConsultaByIdUseCase getConsultaById;

    @Inject
    public HospitalConsultaResource(GetConsultaByIdUseCase getConsultaById) {
        this.getConsultaById = getConsultaById;
    }

    @GET
    @Path("/{consultaId}")
    @Operation(summary = "Obtener detalle de una consulta por id")
    @APIResponse(responseCode = "200", description = "Consulta encontrada")
    @APIResponse(responseCode = "404", description = "Consulta no encontrada")
    public ConsultaResponse getConsulta(@PathParam("consultaId") String consultaId) {
        return HospitalRestMapper.toResponse(getConsultaById.execute(consultaId));
    }
}
