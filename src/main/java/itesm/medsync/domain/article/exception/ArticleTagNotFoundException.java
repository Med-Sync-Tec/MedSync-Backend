package itesm.medsync.domain.article.exception;

import java.util.UUID;

public class ArticleTagNotFoundException extends RuntimeException {

    public ArticleTagNotFoundException(UUID id) {
        super("ArticleTag not found: " + id);
    }
}
