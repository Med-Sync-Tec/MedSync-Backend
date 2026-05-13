package itesm.medsync.application.article;

import itesm.medsync.domain.article.usecase.SaveArticleUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.nio.ByteBuffer;
import java.util.UUID;

@ApplicationScoped
public class SaveArticleService implements SaveArticleUseCase {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public void execute(UUID userId, UUID articleId) {
        // INSERT IGNORE es idempotente: si ya existe, no hace nada
        // Pasamos los UUIDs como byte[] de 16 bytes para que coincidan con la columna BINARY(16)
        em.createNativeQuery(
                "INSERT IGNORE INTO usuario_articulos_guardados (usuario_id, articulo_id, guardado_at) " +
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
