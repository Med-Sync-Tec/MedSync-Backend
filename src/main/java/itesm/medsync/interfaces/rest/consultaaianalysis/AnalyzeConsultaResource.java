package itesm.medsync.interfaces.rest.consultaaianalysis;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.consultaaianalysis.model.ConsultaAnalysis;
import itesm.medsync.domain.consultaaianalysis.usecase.AnalyzeConsultaWithAiUseCase;
import itesm.medsync.domain.user.model.UserWithRole;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST endpoint for AI-backed consulta analysis (review-then-save flow).
 *
 * Read-only: the response carries suggestions the frontend renders for the
 * doctor; persistence happens via the bulk patient-context endpoint when
 * the doctor accepts entries. Path-namespaced under {@code /api/consultas/}
 * to stay decoupled from the existing {@code HospitalResource} CRUD class.
 */
@Path("/api/consultas/{consultaId}/analyze")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Hospital", description = "Consultas del hospital y análisis con IA")
public class AnalyzeConsultaResource {

    private final AnalyzeConsultaWithAiUseCase analyzeConsulta;
    private final AuthenticatedUserContext userContext;

    @Inject
    public AnalyzeConsultaResource(AnalyzeConsultaWithAiUseCase analyzeConsulta,
                                   AuthenticatedUserContext userContext) {
        this.analyzeConsulta = analyzeConsulta;
        this.userContext = userContext;
    }

    @POST
    @Operation(summary = "Analizar una consulta con IA: extrae sugerencias de contexto clínico desde los campos SOAP")
    @APIResponse(responseCode = "200", description = "Análisis exitoso; sugerencias no persistidas")
    @APIResponse(responseCode = "400", description = "Consulta sin texto analizable o usuario sin especialidad")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "404", description = "Consulta no encontrada")
    @APIResponse(responseCode = "502", description = "El proveedor de IA devolvió una respuesta inválida")
    @APIResponse(responseCode = "504", description = "El proveedor de IA excedió el timeout configurado")
    public Response analyze(@PathParam("consultaId") String consultaId) {
        UserWithRole caller = userContext.getCurrentUser();
        if (caller == null) {
            throw new WebApplicationException("Authentication required", Response.Status.UNAUTHORIZED);
        }
        ConsultaAnalysis analysis = analyzeConsulta.execute(consultaId, caller.user());
        return Response.ok(ConsultaAnalysisRestMapper.toResponse(analysis)).build();
    }
}
