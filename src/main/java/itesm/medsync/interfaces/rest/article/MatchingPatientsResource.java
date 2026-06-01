package itesm.medsync.interfaces.rest.article;

import itesm.medsync.domain.patient.usecase.GetMatchingPatientsByArticleUseCase;
import itesm.medsync.interfaces.rest.patient.PatientResponse;
import itesm.medsync.interfaces.rest.patient.PatientRestMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/articles/{articleId}/matching-patients")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Matching Patients", description = "Pacientes relevantes según tags del artículo")
public class MatchingPatientsResource {

    private final GetMatchingPatientsByArticleUseCase getMatching;

    @Inject
    public MatchingPatientsResource(GetMatchingPatientsByArticleUseCase getMatching) {
        this.getMatching = getMatching;
    }

    @GET
    @Operation(summary = "Listar pacientes cuyo contexto clínico coincide con los tags del artículo")
    @APIResponse(responseCode = "200", description = "Lista de pacientes relevantes (cap por 'limit')")
    @APIResponse(responseCode = "404", description = "Artículo no encontrado")
    public List<PatientResponse> getMatching(@PathParam("articleId") UUID articleId,
                                             @QueryParam("limit") @DefaultValue("50") int limit) {
        return getMatching.execute(articleId, limit).stream()
                .map(PatientRestMapper::toResponse)
                .toList();
    }
}
