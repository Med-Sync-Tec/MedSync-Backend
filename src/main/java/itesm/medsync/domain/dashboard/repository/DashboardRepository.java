package itesm.medsync.domain.dashboard.repository;

import itesm.medsync.infrastructure.persistence.article.ArticleEntity;

import java.util.List;
import java.util.UUID;

public interface DashboardRepository {
    List<ArticleEntity> findNovedades48h();
    List<ArticleEntity> findNoLeidos(UUID userId);
    List<ArticleEntity> findPorEspecialidad(UUID userId);
    List<ArticleEntity> findAltaEvidencia();
}
