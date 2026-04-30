package itesm.medsync.domain.article.exception;

public class DuplicateArticleException extends RuntimeException {

    private final String doi;

    public DuplicateArticleException(String doi) {
        super("Article with DOI already exists: " + doi);
        this.doi = doi;
    }

    public String getDoi() {
        return doi;
    }
}
