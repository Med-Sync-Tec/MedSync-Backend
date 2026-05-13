package itesm.medsync.application.dashboard;

import itesm.medsync.domain.dashboard.repository.DashboardRepository;
import itesm.medsync.domain.dashboard.usecase.GetDashboardKpisUseCase;
import itesm.medsync.interfaces.rest.dashboard.DashboardKpiDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

import itesm.medsync.infrastructure.persistence.article.ArticleEntity;
import java.util.List;

@ApplicationScoped
public class GetDashboardKpisUseCaseImpl implements GetDashboardKpisUseCase {

    @Inject
    DashboardRepository dashboardRepository;

    @Override
    public DashboardKpiDTO execute(UUID userId) {
        List<ArticleEntity> novedades48h = dashboardRepository.findNovedades48h();
        List<ArticleEntity> noLeidos = dashboardRepository.findNoLeidos(userId);
        List<ArticleEntity> porEspecialidad = dashboardRepository.findPorEspecialidad(userId);
        List<ArticleEntity> altaEvidencia = dashboardRepository.findAltaEvidencia();

        return new DashboardKpiDTO(novedades48h, noLeidos, porEspecialidad, altaEvidencia);
    }
}
