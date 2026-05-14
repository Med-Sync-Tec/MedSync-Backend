package itesm.medsync.interfaces.rest.articleaianalysis;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.articleaianalysis.model.AnalyzedArticle;
import itesm.medsync.domain.articleaianalysis.usecase.AnalyzeArticleWithAiUseCase;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.usecase.GetSpecialtyByIdUseCase;
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

import java.util.UUID;

/**
 * REST endpoint for AI-backed article analysis.
 *
 * Lives in its own resource class (not as a sub-path of
 * {@code ArticleResource}) so the AI lifecycle stays decoupled from CRUD —
 * future AI-related routes (re-analyze with overrides, dry-run, ...) can land
 * here without bloating {@code ArticleResource}.
 */
@Path("/api/articles/{id}/analyze")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Articles", description = "Artículos científicos y sus tags")
public class AnalyzeArticleResource {

    private final AnalyzeArticleWithAiUseCase analyzeArticle;
    private final GetSpecialtyByIdUseCase getSpecialtyById;
    private final AuthenticatedUserContext userContext;

    @Inject
    public AnalyzeArticleResource(AnalyzeArticleWithAiUseCase analyzeArticle,
                                  GetSpecialtyByIdUseCase getSpecialtyById,
                                  AuthenticatedUserContext userContext) {
        this.analyzeArticle = analyzeArticle;
        this.getSpecialtyById = getSpecialtyById;
        this.userContext = userContext;
    }

    @POST
    @Operation(summary = "Analizar un artículo con IA: clasifica por especialidad y extrae tags del vocabulario")
    @APIResponse(responseCode = "200", description = "Análisis exitoso; tags reemplazados atómicamente")
    @APIResponse(responseCode = "400", description = "Artículo sin texto analizable (título, abstract y keywords vacíos)")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "404", description = "Artículo no encontrado")
    @APIResponse(responseCode = "502", description = "El proveedor de IA devolvió una respuesta inválida")
    @APIResponse(responseCode = "504", description = "El proveedor de IA excedió el timeout configurado")
    public Response analyze(@PathParam("id") UUID id) {
        if (userContext.getCurrentUser() == null) {
            throw new WebApplicationException("Authentication required", Response.Status.UNAUTHORIZED);
        }
        AnalyzedArticle analyzed = analyzeArticle.execute(id);
        Specialty especialidad = getSpecialtyById.execute(analyzed.article().getEspecialidadId());
        return Response.ok(AnalyzeArticleRestMapper.toResponse(analyzed, especialidad)).build();
    }
}
