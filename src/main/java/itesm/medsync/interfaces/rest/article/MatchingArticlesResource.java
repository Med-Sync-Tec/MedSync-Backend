package itesm.medsync.interfaces.rest.article;

import itesm.medsync.domain.article.usecase.GetMatchingArticlesByPatientUseCase;
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

@Path("/api/patients/{patientId}/matching-articles")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Matching Articles", description = "Artículos relevantes según contexto clínico del paciente")
public class MatchingArticlesResource {

    private final GetMatchingArticlesByPatientUseCase getMatching;

    @Inject
    public MatchingArticlesResource(GetMatchingArticlesByPatientUseCase getMatching) {
        this.getMatching = getMatching;
    }

    @GET
    @Operation(summary = "Listar artículos cuyas tags coinciden con el contexto clínico del paciente")
    @APIResponse(responseCode = "200", description = "Lista de artículos relevantes (cap por 'limit')")
    @APIResponse(responseCode = "404", description = "Paciente no encontrado")
    public List<ArticleResponse> getMatching(@PathParam("patientId") UUID patientId,
                                             @QueryParam("limit") @DefaultValue("50") int limit) {
        return getMatching.execute(patientId, limit).stream()
                .map(ArticleRestMapper::toResponse)
                .toList();
    }
}
