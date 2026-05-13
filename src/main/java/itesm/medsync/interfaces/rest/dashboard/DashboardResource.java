package itesm.medsync.interfaces.rest.dashboard;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.dashboard.usecase.GetDashboardKpisUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/dashboard")
public class DashboardResource {

    @Inject
    AuthenticatedUserContext authContext;

    @Inject
    GetDashboardKpisUseCase getDashboardKpisUseCase;

    @GET
    @Path("/kpis")
    @Produces(MediaType.APPLICATION_JSON)
    public DashboardKpiDTO getKpis() {
        return getDashboardKpisUseCase.execute(authContext.getCurrentUser().user().getId());
    }
}
