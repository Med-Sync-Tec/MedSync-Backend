package itesm.medsync.domain.article.usecase;

import java.util.UUID;

public interface UnsaveArticleUseCase {
    void execute(UUID userId, UUID articleId);
}
