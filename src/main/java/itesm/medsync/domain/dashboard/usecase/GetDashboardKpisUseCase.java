package itesm.medsync.domain.dashboard.usecase;

import itesm.medsync.interfaces.rest.dashboard.DashboardKpiDTO;

import java.util.UUID;

public interface GetDashboardKpisUseCase {
    DashboardKpiDTO execute(UUID userId);
}
