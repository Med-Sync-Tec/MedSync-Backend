package itesm.medsync.application.article;

import itesm.medsync.domain.article.usecase.UnsaveArticleUseCase;
import itesm.medsync.infrastructure.persistence.tracking.UsuarioArticuloGuardadoEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UnsaveArticleService implements UnsaveArticleUseCase {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public void execute(UUID userId, UUID articleId) {
        List<UsuarioArticuloGuardadoEntity> results = em.createQuery(
                        "SELECT g FROM UsuarioArticuloGuardadoEntity g " +
                                "WHERE g.usuario.id = :uid AND g.articulo.id = :aid",
                        UsuarioArticuloGuardadoEntity.class)
                .setParameter("uid", userId)
                .setParameter("aid", articleId)
                .getResultList();
        for (UsuarioArticuloGuardadoEntity guardado : results) {
            em.remove(guardado);
        }
    }
}

