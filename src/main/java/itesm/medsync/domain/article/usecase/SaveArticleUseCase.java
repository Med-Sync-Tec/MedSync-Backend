package itesm.medsync.domain.article.usecase;

import java.util.UUID;

public interface SaveArticleUseCase {
    /** Guarda un artículo científico para el usuario dado. Idempotente. */
    void execute(UUID userId, UUID articleId);
}
