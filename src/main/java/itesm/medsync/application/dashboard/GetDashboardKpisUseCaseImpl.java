package itesm.medsync.application.dashboard;

import itesm.medsync.domain.dashboard.model.DashboardKpis;
import itesm.medsync.domain.dashboard.repository.DashboardRepository;
import itesm.medsync.domain.dashboard.usecase.GetDashboardKpisUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Thin orchestrator: delegates each KPI bucket to the repository and
 * bundles the results into the domain {@link DashboardKpis} aggregate.
 *
 * Works exclusively with domain types ({@code Article}, {@link DashboardKpis});
 * no JPA entities or REST DTOs cross this layer.
 */
@ApplicationScoped
public class GetDashboardKpisUseCaseImpl implements GetDashboardKpisUseCase {

    private final DashboardRepository dashboardRepository;

    @Inject
    public GetDashboardKpisUseCaseImpl(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Override
    public DashboardKpis execute(UUID userId) {
        return new DashboardKpis(
                dashboardRepository.findNovedades48h(),
                dashboardRepository.findNoLeidos(userId),
                dashboardRepository.findPorEspecialidad(userId),
                dashboardRepository.findAltaEvidencia());
    }
}
