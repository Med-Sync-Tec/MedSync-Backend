package itesm.medsync.domain.article.usecase;

import java.util.UUID;

public interface UnsaveArticleUseCase {
    /** Elimina un artículo guardado para el usuario dado. Idempotente. */
    void execute(UUID userId, UUID articleId);
}
