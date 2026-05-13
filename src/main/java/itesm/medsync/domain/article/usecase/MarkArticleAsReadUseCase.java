package itesm.medsync.domain.article.usecase;

import java.util.UUID;

public interface MarkArticleAsReadUseCase {
    /** Marca el artículo como leído para el usuario dado. Idempotente. */
    void execute(UUID userId, UUID articleId);
}
