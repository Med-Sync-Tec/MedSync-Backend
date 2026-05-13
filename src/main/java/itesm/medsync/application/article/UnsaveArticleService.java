package itesm.medsync.application.article;

import itesm.medsync.domain.article.usecase.UnsaveArticleUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.nio.ByteBuffer;
import java.util.UUID;

@ApplicationScoped
public class UnsaveArticleService implements UnsaveArticleUseCase {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public void execute(UUID userId, UUID articleId) {
        em.createNativeQuery(
                "DELETE FROM usuario_articulos_guardados WHERE usuario_id = :userId AND articulo_id = :articleId")
          .setParameter("userId", toBytes(userId))
          .setParameter("articleId", toBytes(articleId))
          .executeUpdate();
    }

    private byte[] toBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
