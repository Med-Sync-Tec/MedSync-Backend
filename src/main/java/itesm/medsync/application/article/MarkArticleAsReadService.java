package itesm.medsync.application.article;

import itesm.medsync.domain.article.usecase.MarkArticleAsReadUseCase;
import itesm.medsync.infrastructure.persistence.article.ArticleEntity;
import itesm.medsync.infrastructure.persistence.tracking.UsuarioArticuloLeidoEntity;
import itesm.medsync.infrastructure.persistence.tracking.UsuarioArticuloLeidoId;
import itesm.medsync.infrastructure.persistence.user.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class MarkArticleAsReadService implements MarkArticleAsReadUseCase {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public void execute(UUID userId, UUID articleId) {
        UsuarioArticuloLeidoId pk = new UsuarioArticuloLeidoId(userId, articleId);
        // Si ya existe el registro, no hacemos nada (idempotente)
        if (em.find(UsuarioArticuloLeidoEntity.class, pk) != null) {
            return;
        }
        UserEntity usuario = em.getReference(UserEntity.class, userId);
        ArticleEntity articulo = em.getReference(ArticleEntity.class, articleId);

        UsuarioArticuloLeidoEntity leido = new UsuarioArticuloLeidoEntity();
        leido.setUsuario(usuario);
        leido.setArticulo(articulo);
        em.persist(leido);
    }
}
