package itesm.medsync.infrastructure.persistence.dashboard;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.dashboard.repository.DashboardRepository;
import itesm.medsync.infrastructure.persistence.article.ArticleEntity;
import itesm.medsync.infrastructure.persistence.user.UserEntity;
import itesm.medsync.infrastructure.persistence.user.UserRepositoryImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DashboardRepositoryImpl implements DashboardRepository, PanacheRepositoryBase<ArticleEntity, UUID> {

    @Inject
    UserRepositoryImpl userRepository;

    @Override
    public List<ArticleEntity> findNovedades48h() {
        LocalDateTime minus48Hours = LocalDateTime.now().minusHours(48);
        return find("createdAt >= ?1", minus48Hours).list();
    }

    @Override
    public List<ArticleEntity> findNoLeidos(UUID userId) {
        return find("id NOT IN (SELECT ual.articulo.id FROM UsuarioArticuloLeidoEntity ual WHERE ual.usuario.id = ?1)",
                userId).list();
    }

    @Override
    public List<ArticleEntity> findPorEspecialidad(UUID userId) {
        UserEntity user = userRepository.findById(userId);
        if (user == null || user.getEspecialidad() == null || user.getEspecialidad().isBlank()) {
            return List.of();
        }

        return find("from ArticleEntity a join a.tags t where t.valor = ?1", user.getEspecialidad()).list();
    }

    @Override
    public List<ArticleEntity> findAltaEvidencia() {
        return find("tipoPublicacion = ?1", "Journal Article").list();
    }
}
