package itesm.medsync.interfaces.rest.patient;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.usecase.CreatePatientUseCase;
import itesm.medsync.domain.patient.usecase.DeletePatientUseCase;
import itesm.medsync.domain.patient.usecase.GetPatientByIdUseCase;
import itesm.medsync.domain.patient.usecase.ListActivePatientsUseCase;
import itesm.medsync.domain.user.model.UserWithRole;
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

@Path("/api/patients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Patients", description = "Gestión de pacientes")
public class PatientResource {

    private final CreatePatientUseCase createPatient;
    private final ListActivePatientsUseCase listActivePatients;
    private final GetPatientByIdUseCase getPatientById;
    private final DeletePatientUseCase deletePatient;
    private final AuthenticatedUserContext userContext;

    @Inject
    public PatientResource(CreatePatientUseCase createPatient,
                           ListActivePatientsUseCase listActivePatients,
                           GetPatientByIdUseCase getPatientById,
                           DeletePatientUseCase deletePatient,
                           AuthenticatedUserContext userContext) {
        this.createPatient = createPatient;
        this.listActivePatients = listActivePatients;
        this.getPatientById = getPatientById;
        this.deletePatient = deletePatient;
        this.userContext = userContext;
    }

    @POST
    @Operation(summary = "Crear paciente asignado al médico autenticado")
    @APIResponse(responseCode = "201", description = "Paciente creado")
    @APIResponse(responseCode = "400", description = "Datos inválidos")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "409", description = "Expediente duplicado")
    public Response create(@Valid CreatePatientRequest request, @Context UriInfo uriInfo) {
        UserWithRole medico = userContext.getCurrentUser();
        if (medico == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Patient created = createPatient.execute(
                request.expedienteExternoId,
                request.nombre,
                request.fechaNacimiento,
                request.genero,
                medico.user().getId()
        );
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getId().toString()).build();
        return Response.created(location)
                .entity(PatientRestMapper.toResponse(created))
                .build();
    }

    @GET
    @Operation(summary = "Listar pacientes activos")
    @APIResponse(responseCode = "200", description = "Lista de pacientes activos")
    public List<PatientResponse> listActive() {
        return listActivePatients.execute().stream()
                .map(PatientRestMapper::toResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Obtener paciente por id")
    @APIResponse(responseCode = "200")
    @APIResponse(responseCode = "404", description = "Paciente no encontrado")
    public PatientResponse getById(@PathParam("id") UUID id) {
        return PatientRestMapper.toResponse(getPatientById.execute(id));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Soft delete de paciente")
    @APIResponse(responseCode = "204", description = "Paciente marcado como inactivo")
    @APIResponse(responseCode = "404", description = "Paciente no encontrado")
    public Response delete(@PathParam("id") UUID id) {
        deletePatient.execute(id);
        return Response.noContent().build();
    }
}
