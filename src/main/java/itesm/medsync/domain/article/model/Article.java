package itesm.medsync.domain.article.model;

import itesm.medsync.domain.article.exception.InvalidArticleDataException;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Article {

    private static final int MAX_TITULO_LENGTH = 1000;
    private static final int MAX_DOI_LENGTH = 200;
    private static final int MAX_URL_LENGTH = 500;

    private final UUID id;
    private final String titulo;
    private final String autores;
    private final String revista;
    private final Integer anioPub;
    private final String mesPub;
    private final String doi;
    private final String abstractText;
    private final String keywords;
    private final String tipoPublicacion;
    private final String url;
    private final List<ArticleTag> tags;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Article(UUID id,
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
                   List<ArticleTag> tags,
                   LocalDateTime createdAt,
                   LocalDateTime updatedAt) {
        validate(id, titulo, anioPub, doi, url);
        this.id = id;
        this.titulo = titulo.trim();
        this.autores = autores;
        this.revista = revista;
        this.anioPub = anioPub;
        this.mesPub = mesPub;
        this.doi = doi == null || doi.isBlank() ? null : doi.trim();
        this.abstractText = abstractText;
        this.keywords = keywords;
        this.tipoPublicacion = tipoPublicacion;
        this.url = url == null || url.isBlank() ? null : url.trim();
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Article create(String titulo,
                                 String autores,
                                 String revista,
                                 Integer anioPub,
                                 String mesPub,
                                 String doi,
                                 String abstractText,
                                 String keywords,
                                 String tipoPublicacion,
                                 String url) {
        return new Article(
                UUID.randomUUID(),
                titulo, autores, revista, anioPub, mesPub,
                doi, abstractText, keywords, tipoPublicacion, url,
                new ArrayList<>(),
                null, null);
    }

    public Article withTagAdded(ArticleTag tag) {
        if (tag == null) {
            throw new InvalidArticleDataException("tag cannot be null");
        }
        List<ArticleTag> next = new ArrayList<>(this.tags);
        next.add(tag);
        return new Article(
                id, titulo, autores, revista, anioPub, mesPub,
                doi, abstractText, keywords, tipoPublicacion, url,
                next, createdAt, updatedAt);
    }

    public Article withTagRemoved(UUID tagId) {
        List<ArticleTag> next = new ArrayList<>(this.tags);
        next.removeIf(t -> t.getId().equals(tagId));
        return new Article(
                id, titulo, autores, revista, anioPub, mesPub,
                doi, abstractText, keywords, tipoPublicacion, url,
                next, createdAt, updatedAt);
    }

    private static void validate(UUID id, String titulo, Integer anioPub, String doi, String url) {
        if (id == null) {
            throw new InvalidArticleDataException("Article id cannot be null");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new InvalidArticleDataException("titulo cannot be null or blank");
        }
        if (titulo.length() > MAX_TITULO_LENGTH) {
            throw new InvalidArticleDataException(
                    "titulo cannot exceed " + MAX_TITULO_LENGTH + " characters");
        }
        if (anioPub != null) {
            int currentYear = Year.now().getValue();
            if (anioPub < 1800 || anioPub > currentYear + 1) {
                throw new InvalidArticleDataException(
                        "anioPub must be between 1800 and " + (currentYear + 1));
            }
        }
        if (doi != null && doi.length() > MAX_DOI_LENGTH) {
            throw new InvalidArticleDataException(
                    "doi cannot exceed " + MAX_DOI_LENGTH + " characters");
        }
        if (url != null && url.length() > MAX_URL_LENGTH) {
            throw new InvalidArticleDataException(
                    "url cannot exceed " + MAX_URL_LENGTH + " characters");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutores() {
        return autores;
    }

    public String getRevista() {
        return revista;
    }

    public Integer getAnioPub() {
        return anioPub;
    }

    public String getMesPub() {
        return mesPub;
    }

    public String getDoi() {
        return doi;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getTipoPublicacion() {
        return tipoPublicacion;
    }

    public String getUrl() {
        return url;
    }

    public List<ArticleTag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
