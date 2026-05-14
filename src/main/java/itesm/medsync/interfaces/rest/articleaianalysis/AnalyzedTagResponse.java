package itesm.medsync.interfaces.rest.articleaianalysis;

import java.util.UUID;

/**
 * Single AI-extracted tag projected for REST. Mirrors the shape of
 * {@code ArticleTagResponse} from the article feature so the frontend can
 * reuse its tag-rendering component without a discriminator.
 */
public record AnalyzedTagResponse(UUID id, String tipo, String valor) {
}
