package itesm.medsync.application.article;

import itesm.medsync.domain.article.usecase.MarkArticleAsReadUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.nio.ByteBuffer;
import java.util.UUID;

@ApplicationScoped
public class MarkArticleAsReadService implements MarkArticleAsReadUseCase {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public void execute(UUID userId, UUID articleId) {
        // INSERT IGNORE es idempotente: si ya existe la entrada, no hace nada
        // Usamos byte[] de 16 bytes para que coincida con la columna BINARY(16)
        em.createNativeQuery(
                "INSERT IGNORE INTO usuario_articulos_leidos (usuario_id, articulo_id, leido_at) " +
                "VALUES (:userId, :articleId, NOW())")
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
