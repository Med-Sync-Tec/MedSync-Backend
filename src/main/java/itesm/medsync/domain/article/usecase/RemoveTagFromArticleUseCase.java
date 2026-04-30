package itesm.medsync.domain.article.usecase;

import java.util.UUID;

public interface RemoveTagFromArticleUseCase {

    void execute(UUID articleId, UUID tagId);
}
