package itesm.medsync.interfaces.rest.article;

import java.util.List;

public class PagedArticlesResponse {
    public List<ArticleResponse> items;
    public long total;
    public int page;
    public int size;

    public PagedArticlesResponse() { }

    public PagedArticlesResponse(List<ArticleResponse> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}
