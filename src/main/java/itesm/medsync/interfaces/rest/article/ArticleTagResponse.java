package itesm.medsync.interfaces.rest.article;

import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleTagResponse(
        UUID id,
        String tipo,
        String valor,
        LocalDateTime createdAt
) {
}
