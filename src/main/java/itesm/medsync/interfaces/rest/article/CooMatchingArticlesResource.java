package itesm.medsync.interfaces.rest.article;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.article.usecase.GetMatchingArticlesForMedicamentosUseCase;
import itesm.medsync.domain.user.model.KnownRoles;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.interfaces.rest.common.ErrorResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * Artículos relevantes para el COO: los que mencionan (tag MEDICAMENTO) algún
 * medicamento del catálogo. Espejo de {@link MatchingArticlesResource} pero
 * usando el catálogo completo como contexto. Restringido al rol COO.
 */
@Path("/api/coo/matching-articles")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Matching Articles", description = "Artículos relevantes según el catálogo de medicamentos (COO)")
public class CooMatchingArticlesResource {

    private final GetMatchingArticlesForMedicamentosUseCase getMatching;
    private final AuthenticatedUserContext userContext;

    @Inject
    public CooMatchingArticlesResource(GetMatchingArticlesForMedicamentosUseCase getMatching,
                                       AuthenticatedUserContext userContext) {
        this.getMatching = getMatching;
        this.userContext = userContext;
    }

    @GET
    @Operation(summary = "Listar artículos cuyos tags de medicamento coinciden con el catálogo (COO)")
    @APIResponse(responseCode = "200", description = "Lista de artículos relevantes (cap por 'limit')")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "Solo el COO puede consultar este match")
    public Response getMatching(@QueryParam("limit") @DefaultValue("50") int limit) {
        Response auth = enforceCoo();
        if (auth != null) return auth;

        List<ArticleResponse> body = getMatching.execute(limit).stream()
                .map(ArticleRestMapper::toResponse)
                .toList();
        return Response.ok(body).build();
    }

    private Response enforceCoo() {
        UserWithRole caller = userContext.getCurrentUser();
        if (caller == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (!KnownRoles.COO.equalsIgnoreCase(caller.roleName())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse(403, "Forbidden",
                            "Solo el COO puede consultar las noticias por medicamento"))
                    .build();
        }
        return null;
    }
}
