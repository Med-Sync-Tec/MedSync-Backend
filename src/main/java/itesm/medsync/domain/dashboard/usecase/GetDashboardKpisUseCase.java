package itesm.medsync.domain.dashboard.usecase;

import itesm.medsync.domain.dashboard.model.DashboardKpis;

import java.util.UUID;

/**
 * Returns the four KPI buckets for the dashboard, scoped to the
 * authenticated user (the {@code noLeidos} and {@code porEspecialidad}
 * buckets are user-dependent; the other two are global).
 */
public interface GetDashboardKpisUseCase {

    DashboardKpis execute(UUID userId);
}
