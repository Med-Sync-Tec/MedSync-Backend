package itesm.medsync.domain.article.usecase;

import java.util.UUID;

public interface SaveArticleUseCase {
    void execute(UUID userId, UUID articleId);
}
