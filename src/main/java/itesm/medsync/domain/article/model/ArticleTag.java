package itesm.medsync.domain.article.model;

import itesm.medsync.domain.article.exception.InvalidArticleDataException;
import itesm.medsync.domain.shared.model.TipoClinico;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class ArticleTag {

    private static final int MAX_VALOR_LENGTH = 500;

    private final UUID id;
    private final TipoClinico tipo;
    private final String valor;
    private final LocalDateTime createdAt;

    public ArticleTag(UUID id, TipoClinico tipo, String valor, LocalDateTime createdAt) {
        validate(id, tipo, valor);
        this.id = id;
        this.tipo = tipo;
        this.valor = valor.trim();
        this.createdAt = createdAt;
    }

    public static ArticleTag create(TipoClinico tipo, String valor) {
        return new ArticleTag(UUID.randomUUID(), tipo, valor, null);
    }

    private static void validate(UUID id, TipoClinico tipo, String valor) {
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
    }

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
