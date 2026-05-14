package itesm.medsync.domain.vocabulary.model;

import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.vocabulary.exception.InvalidVocabularyTermDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class VocabularyTermTest {

    @Test
    @DisplayName("Constructor válido conserva el valor exactamente como se recibe")
    void happyPathPreservesValor() {
        VocabularyTerm term = new VocabularyTerm(TipoClinico.ENFERMEDAD, "Insuficiencia cardíaca");

        assertEquals(TipoClinico.ENFERMEDAD, term.getTipo());
        assertEquals("Insuficiencia cardíaca", term.getValor());
    }

    @Test
    @DisplayName("Constructor no recorta espacios — preserva el valor original")
    void doesNotTrimValor() {
        VocabularyTerm term = new VocabularyTerm(TipoClinico.SINTOMA, "  Disnea  ");
        assertEquals("  Disnea  ", term.getValor());
    }

    @Test
    @DisplayName("tipo null lanza InvalidVocabularyTermDataException")
    void tipoNull() {
        assertThrows(InvalidVocabularyTermDataException.class,
                () -> new VocabularyTerm(null, "Disnea"));
    }

    @ParameterizedTest(name = "valor inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "   \t  "})
    @DisplayName("valor null/blank/whitespace-only lanza InvalidVocabularyTermDataException")
    void valorBlank(String invalid) {
        assertThrows(InvalidVocabularyTermDataException.class,
                () -> new VocabularyTerm(TipoClinico.ENFERMEDAD, invalid));
    }

    @Test
    @DisplayName("valor > 500 chars lanza InvalidVocabularyTermDataException")
    void valorTooLong() {
        String tooLong = "a".repeat(501);
        assertThrows(InvalidVocabularyTermDataException.class,
                () -> new VocabularyTerm(TipoClinico.ENFERMEDAD, tooLong));
    }

    @Test
    @DisplayName("valor exactamente 500 chars es válido")
    void valorAtLimit() {
        String atLimit = "a".repeat(500);
        assertDoesNotThrow(() -> new VocabularyTerm(TipoClinico.ENFERMEDAD, atLimit));
    }

    @Test
    @DisplayName("equals/hashCode basados en (tipo, valor) case-sensitive")
    void equalsAndHashCode() {
        VocabularyTerm a = new VocabularyTerm(TipoClinico.ENFERMEDAD, "Disnea");
        VocabularyTerm b = new VocabularyTerm(TipoClinico.ENFERMEDAD, "Disnea");
        VocabularyTerm c = new VocabularyTerm(TipoClinico.SINTOMA, "Disnea");
        VocabularyTerm d = new VocabularyTerm(TipoClinico.ENFERMEDAD, "Otro");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, d);
    }
}
