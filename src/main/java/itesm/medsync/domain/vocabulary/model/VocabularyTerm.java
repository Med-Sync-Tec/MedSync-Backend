package itesm.medsync.domain.vocabulary.model;

import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.vocabulary.exception.InvalidVocabularyTermDataException;

import java.util.Objects;

/**
 * A single clinical term inside a {@link Vocabulary} — for example,
 * {@code (ENFERMEDAD, "Insuficiencia cardíaca")}.
 *
 * Value object: identity is the {@code (tipo, valor)} pair, not a database id.
 * Terms are not persisted as rows — they live inside the per-specialty JSON
 * vocabulary files and are loaded into memory at startup.
 *
 * The constructor preserves {@code valor} verbatim (no trimming, no
 * normalization). Case-insensitive matching is done at the {@link Vocabulary}
 * level so the canonical form remains available for prompt-building and the
 * debug endpoint.
 */
public final class VocabularyTerm {

    public static final int MAX_VALOR_LENGTH = 500;

    private final TipoClinico tipo;
    private final String valor;

    public VocabularyTerm(TipoClinico tipo, String valor) {
        if (tipo == null) {
            throw new InvalidVocabularyTermDataException("tipo cannot be null");
        }
        if (valor == null || valor.isBlank()) {
            throw new InvalidVocabularyTermDataException("valor cannot be null or blank");
        }
        if (valor.length() > MAX_VALOR_LENGTH) {
            throw new InvalidVocabularyTermDataException(
                    "valor cannot exceed " + MAX_VALOR_LENGTH + " characters");
        }
        this.tipo = tipo;
        this.valor = valor;
    }

    public TipoClinico getTipo() {
        return tipo;
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VocabularyTerm other)) return false;
        return tipo == other.tipo && Objects.equals(valor, other.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, valor);
    }
}
