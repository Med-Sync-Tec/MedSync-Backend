package itesm.medsync.domain.article.model;

import itesm.medsync.domain.article.exception.InvalidArticleDataException;
import itesm.medsync.domain.article.usecase.CreateArticleCommand;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Article(
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
        List<ArticleTag> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static final int MAX_TITULO_LENGTH = 1000;
    public static final int MAX_DOI_LENGTH = 200;
    public static final int MAX_URL_LENGTH = 500;

    // Compact constructor: validate, then normalize fields and defensively copy tags
    public Article {
        validate(id, titulo, anioPub, doi, url);
        titulo = titulo.trim();
        doi = (doi == null || doi.isBlank()) ? null : doi.trim();
        url = (url == null || url.isBlank()) ? null : url.trim();
        // Defensive copy — List.copyOf is unmodifiable and null-safe via null check
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public static Article create(CreateArticleCommand cmd) {
        return new Article(UUID.randomUUID(), cmd.titulo(), cmd.autores(), cmd.revista(),
                cmd.anioPub(), cmd.mesPub(), cmd.doi(), cmd.abstractText(),
                cmd.keywords(), cmd.tipoPublicacion(), cmd.url(), null, null, null, null);
    }

    public Article withTagAdded(ArticleTag tag) {
        if (tag == null) {
            throw new InvalidArticleDataException("tag cannot be null");
        }
        List<ArticleTag> next = new java.util.ArrayList<>(this.tags);
        next.add(tag);
        return new Article(
                id, titulo, autores, revista, anioPub, mesPub,
                doi, abstractText, keywords, tipoPublicacion, url,
                especialidadId, next, createdAt, updatedAt);
    }

    /**
     * Returns a copy with {@code especialidadId} set and the tag list fully replaced.
     *
     * Used by the AI analysis flow (feature 3) to apply atomic
     * "classify + extract" results — prior AI- or manually-added tags are dropped
     * in favor of the LLM's current best answer. The atomic-replacement intent
     * lives on the domain (not on the service) so it remains a single domain
     * operation that future tests can construct without going through the gateway.
     */
    public Article withAiAnalysis(UUID newEspecialidadId, List<ArticleTag> newTags) {
        if (newEspecialidadId == null) {
            throw new InvalidArticleDataException("especialidadId cannot be null");
        }
        if (newTags == null) {
            throw new InvalidArticleDataException("tags cannot be null");
        }
        return new Article(
                id, titulo, autores, revista, anioPub, mesPub,
                doi, abstractText, keywords, tipoPublicacion, url,
                newEspecialidadId, new java.util.ArrayList<>(newTags), createdAt, updatedAt);
    }

    public Article withTagRemoved(UUID tagId) {
        List<ArticleTag> next = new java.util.ArrayList<>(this.tags);
        next.removeIf(t -> t.getId().equals(tagId));
        return new Article(
                id, titulo, autores, revista, anioPub, mesPub,
                doi, abstractText, keywords, tipoPublicacion, url,
                especialidadId, next, createdAt, updatedAt);
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
            int currentYear = Year.now(ZoneOffset.UTC).getValue();
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

    // Backward-compatible accessors
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

    public UUID getEspecialidadId() {
        return especialidadId;
    }

    // tags is already List.copyOf(...) from the compact constructor — already unmodifiable
    public List<ArticleTag> getTags() {
        return tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Id-based equality
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
