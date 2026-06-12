package itesm.medsync.interfaces.rest.article;

import java.util.List;

public record PagedArticlesResponse(List<ArticleResponse> items, long total, int page, int size) {
}
