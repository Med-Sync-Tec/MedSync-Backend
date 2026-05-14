package itesm.medsync.interfaces.rest.dashboard;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.dashboard.model.DashboardKpis;
import itesm.medsync.domain.dashboard.usecase.GetDashboardKpisUseCase;
import itesm.medsync.interfaces.rest.article.ArticleResponse;
import itesm.medsync.interfaces.rest.article.ArticleRestMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/v1/dashboard")
public class DashboardResource {

    private final AuthenticatedUserContext authContext;
    private final GetDashboardKpisUseCase getDashboardKpisUseCase;

    @Inject
    public DashboardResource(AuthenticatedUserContext authContext,
                             GetDashboardKpisUseCase getDashboardKpisUseCase) {
        this.authContext = authContext;
        this.getDashboardKpisUseCase = getDashboardKpisUseCase;
    }

    @GET
    @Path("/kpis")
    @Produces(MediaType.APPLICATION_JSON)
    public DashboardKpiDTO getKpis() {
        DashboardKpis kpis = getDashboardKpisUseCase.execute(
                authContext.getCurrentUser().user().getId());
        return new DashboardKpiDTO(
                toResponses(kpis.novedades48h()),
                toResponses(kpis.noLeidos()),
                toResponses(kpis.porEspecialidad()),
                toResponses(kpis.altaEvidencia()));
    }

    private static List<ArticleResponse> toResponses(List<Article> articles) {
        return articles.stream().map(ArticleRestMapper::toResponse).toList();
    }
}
