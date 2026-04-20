package itesm.medsync.interfaces.rest.hospital;

import itesm.medsync.domain.hospital.usecase.GetConsultasByPatientUseCase;
import itesm.medsync.domain.hospital.usecase.GetExpedienteByPatientUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/patients/{id}")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Hospital", description = "Integración read-only con el sistema del hospital externo")
public class HospitalResource {

    private final GetExpedienteByPatientUseCase getExpedienteByPatient;
    private final GetConsultasByPatientUseCase getConsultasByPatient;

    @Inject
    public HospitalResource(GetExpedienteByPatientUseCase getExpedienteByPatient,
                            GetConsultasByPatientUseCase getConsultasByPatient) {
        this.getExpedienteByPatient = getExpedienteByPatient;
        this.getConsultasByPatient = getConsultasByPatient;
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
}
