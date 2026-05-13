package itesm.medsync.domain.vocabulary.model;

import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.vocabulary.exception.InvalidVocabularyTermDataException;

import java.util.Objects;

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
