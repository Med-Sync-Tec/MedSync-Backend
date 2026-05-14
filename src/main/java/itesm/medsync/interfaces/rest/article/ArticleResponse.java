package itesm.medsync.interfaces.rest.article;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ArticleResponse(
        UUID id,
        String titulo,
        String autores,
        String revista,
        Integer anioPub,
        String mesPub,
        String doi,
        String abstractText,
        String keywords,
        String tipoPublicacion,
        String url,
        UUID especialidadId,
        List<ArticleTagResponse> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
