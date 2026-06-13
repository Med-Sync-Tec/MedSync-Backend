package itesm.medsync.domain.article.model;

import itesm.medsync.domain.article.exception.InvalidArticleDataException;
import itesm.medsync.domain.shared.model.TipoClinico;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record ArticleTag(UUID id, TipoClinico tipo, String valor, LocalDateTime createdAt) {

    private static final int MAX_VALOR_LENGTH = 500;

    // Compact constructor: validate first, then normalize
    public ArticleTag {
        if (id == null) {
            throw new InvalidArticleDataException("ArticleTag id cannot be null");
        }
        if (tipo == null) {
            throw new InvalidArticleDataException("ArticleTag tipo cannot be null");
        }
        if (valor == null || valor.isBlank()) {
            throw new InvalidArticleDataException("ArticleTag valor cannot be null or blank");
        }
        if (valor.length() > MAX_VALOR_LENGTH) {
            throw new InvalidArticleDataException(
                    "ArticleTag valor cannot exceed " + MAX_VALOR_LENGTH + " characters");
        }
        // Normalize after validation
        valor = valor.trim();
    }

    public static ArticleTag create(TipoClinico tipo, String valor) {
        return new ArticleTag(UUID.randomUUID(), tipo, valor, null);
    }

    // Backward-compatible accessors
    public UUID getId() {
        return id;
    }

    public TipoClinico getTipo() {
        return tipo;
    }

    public String getValor() {
        return valor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Id-based equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArticleTag other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
