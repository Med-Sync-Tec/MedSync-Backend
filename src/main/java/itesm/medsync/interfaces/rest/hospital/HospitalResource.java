package itesm.medsync.interfaces.rest.hospital;

import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.usecase.CreateConsultaCommand;
import itesm.medsync.domain.hospital.usecase.CreateConsultaUseCase;
import itesm.medsync.domain.hospital.usecase.GetConsultasByPatientUseCase;
import itesm.medsync.domain.hospital.usecase.GetExpedienteByPatientUseCase;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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

@Path("/api/patients/{id}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Hospital", description = "Integración con el sistema del hospital externo")
public class HospitalResource {

    private final GetExpedienteByPatientUseCase getExpedienteByPatient;
    private final GetConsultasByPatientUseCase getConsultasByPatient;
    private final CreateConsultaUseCase createConsulta;

    @Inject
    public HospitalResource(GetExpedienteByPatientUseCase getExpedienteByPatient,
                            GetConsultasByPatientUseCase getConsultasByPatient,
                            CreateConsultaUseCase createConsulta) {
        this.getExpedienteByPatient = getExpedienteByPatient;
        this.getConsultasByPatient = getConsultasByPatient;
        this.createConsulta = createConsulta;
    }

    @GET
    @Path("/expediente")
    @Operation(summary = "Obtener expediente clínico del paciente en el hospital")
    @APIResponse(responseCode = "200", description = "Expediente encontrado")
    @APIResponse(responseCode = "404", description = "Paciente o expediente no encontrado")
    public ExpedienteClinicoResponse getExpediente(@PathParam("id") UUID patientId) {
        return HospitalRestMapper.toResponse(getExpedienteByPatient.execute(patientId));
    }

    @GET
    @Path("/consultas")
    @Operation(summary = "Listar consultas SOAP del paciente en el hospital")
    @APIResponse(responseCode = "200", description = "Lista de consultas (puede estar vacía)")
    @APIResponse(responseCode = "404", description = "Paciente no encontrado")
    public List<ConsultaResponse> getConsultas(@PathParam("id") UUID patientId) {
        return HospitalRestMapper.toResponseList(getConsultasByPatient.execute(patientId));
    }

    @POST
    @Path("/consultas")
    @Operation(summary = "Crear consulta SOAP para el paciente (auto-crea expediente si no existe)")
    @APIResponse(responseCode = "201", description = "Consulta creada")
    @APIResponse(responseCode = "400", description = "Datos inválidos")
    @APIResponse(responseCode = "404", description = "Paciente no encontrado")
    public Response createConsulta(@PathParam("id") UUID patientId,
                                   @Valid CreateConsultaRequest request,
                                   @Context UriInfo uriInfo) {
        CreateConsultaCommand cmd = new CreateConsultaCommand(
                request.fecha, request.motivoConsulta, request.subjetivo,
                request.objetivo, request.evaluacion, request.plan,
                request.prescripcion, request.diagnostico);
        Consulta created = createConsulta.execute(patientId, cmd);
        URI location = uriInfo.getBaseUriBuilder()
                .path("api").path("consultas").path(created.getId()).build();
        return Response.created(location)
                .entity(HospitalRestMapper.toResponse(created))
                .build();
    }
}
