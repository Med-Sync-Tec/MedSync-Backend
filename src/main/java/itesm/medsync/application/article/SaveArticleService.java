package itesm.medsync.application.article;

import itesm.medsync.domain.article.usecase.SaveArticleUseCase;
import itesm.medsync.infrastructure.persistence.article.ArticleEntity;
import itesm.medsync.infrastructure.persistence.tracking.UsuarioArticuloGuardadoEntity;
import itesm.medsync.infrastructure.persistence.user.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class SaveArticleService implements SaveArticleUseCase {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public void execute(UUID userId, UUID articleId) {
        Long count = em.createQuery(
                        "SELECT COUNT(g) FROM UsuarioArticuloGuardadoEntity g " +
                                "WHERE g.usuario.id = :uid AND g.articulo.id = :aid", Long.class)
                .setParameter("uid", userId)
                .setParameter("aid", articleId)
                .getSingleResult();
        if (count > 0) {
            return;
        }
        UserEntity usuario = em.getReference(UserEntity.class, userId);
        ArticleEntity articulo = em.getReference(ArticleEntity.class, articleId);

        UsuarioArticuloGuardadoEntity guardado = new UsuarioArticuloGuardadoEntity();
        guardado.setUsuario(usuario);
        guardado.setArticulo(articulo);
        em.persist(guardado);
    }
}
