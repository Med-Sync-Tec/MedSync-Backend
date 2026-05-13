package itesm.medsync.domain.vocabulary.model;

import itesm.medsync.domain.shared.model.TipoClinico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VocabularyTest {

    private static final UUID ID = UUID.randomUUID();

    private VocabularyTerm term(TipoClinico tipo, String valor) {
        return new VocabularyTerm(tipo, valor);
    }

    @Test
    @DisplayName("Constructor normaliza el mapa — los 4 TipoClinico siempre están presentes")
    void normalizesAllTipoClinicoKeys() {
        Map<TipoClinico, List<VocabularyTerm>> input = new EnumMap<>(TipoClinico.class);
        input.put(TipoClinico.ENFERMEDAD, List.of(term(TipoClinico.ENFERMEDAD, "Hipertensión")));

        Vocabulary v = new Vocabulary(ID, "cardiologia", "2026-05-12", input);

        assertEquals(1, v.getTermsFor(TipoClinico.ENFERMEDAD).size());
        assertNotNull(v.getTermsFor(TipoClinico.SINTOMA));
        assertNotNull(v.getTermsFor(TipoClinico.TRATAMIENTO));
        assertNotNull(v.getTermsFor(TipoClinico.MEDICAMENTO));
        assertTrue(v.getTermsFor(TipoClinico.SINTOMA).isEmpty());
        assertTrue(v.getTermsFor(TipoClinico.TRATAMIENTO).isEmpty());
        assertTrue(v.getTermsFor(TipoClinico.MEDICAMENTO).isEmpty());
    }

    @Test
    @DisplayName("empty() devuelve aggregate con version 'empty' y buckets vacíos")
    void emptyFactory() {
        Vocabulary v = Vocabulary.empty(ID, "cardiologia");

        assertEquals(ID, v.getEspecialidadId());
        assertEquals("cardiologia", v.getEspecialidadSlug());
        assertEquals("empty", v.getVersion());
        for (TipoClinico tipo : TipoClinico.values()) {
            assertTrue(v.getTermsFor(tipo).isEmpty());
        }
        assertEquals(0, v.totalTerms());
    }

    @Test
    @DisplayName("totalTerms suma los tamaños de todos los buckets")
    void totalTermsSums() {
        Map<TipoClinico, List<VocabularyTerm>> input = new EnumMap<>(TipoClinico.class);
        input.put(TipoClinico.ENFERMEDAD, List.of(
                term(TipoClinico.ENFERMEDAD, "A"),
                term(TipoClinico.ENFERMEDAD, "B")));
        input.put(TipoClinico.SINTOMA, List.of(term(TipoClinico.SINTOMA, "C")));
        input.put(TipoClinico.TRATAMIENTO, List.of());
        input.put(TipoClinico.MEDICAMENTO, List.of(
                term(TipoClinico.MEDICAMENTO, "D"),
                term(TipoClinico.MEDICAMENTO, "E"),
                term(TipoClinico.MEDICAMENTO, "F")));

        Vocabulary v = new Vocabulary(ID, "cardiologia", "v1", input);

        assertEquals(6, v.totalTerms());
    }

    @Test
    @DisplayName("getTermsFor devuelve el bucket correcto")
    void getTermsForReturnsRightBucket() {
        Map<TipoClinico, List<VocabularyTerm>> input = new EnumMap<>(TipoClinico.class);
        VocabularyTerm sintoma = term(TipoClinico.SINTOMA, "Cefalea");
        input.put(TipoClinico.SINTOMA, List.of(sintoma));
        Vocabulary v = new Vocabulary(ID, "neurologia", "v1", input);

        List<VocabularyTerm> sintomas = v.getTermsFor(TipoClinico.SINTOMA);
        assertEquals(1, sintomas.size());
        assertEquals(sintoma, sintomas.get(0));
    }

    @Test
    @DisplayName("containsTerm es case-insensitive y trim-tolerant")
    void containsTermCaseAndTrimInsensitive() {
        Map<TipoClinico, List<VocabularyTerm>> input = new EnumMap<>(TipoClinico.class);
        input.put(TipoClinico.ENFERMEDAD, List.of(term(TipoClinico.ENFERMEDAD, "Insuficiencia cardíaca")));
        Vocabulary v = new Vocabulary(ID, "cardiologia", "v1", input);

        assertTrue(v.containsTerm(TipoClinico.ENFERMEDAD, "Insuficiencia cardíaca"));
        assertTrue(v.containsTerm(TipoClinico.ENFERMEDAD, "insuficiencia cardíaca"));
        assertTrue(v.containsTerm(TipoClinico.ENFERMEDAD, "INSUFICIENCIA CARDÍACA"));
        assertTrue(v.containsTerm(TipoClinico.ENFERMEDAD, "  Insuficiencia cardíaca  "));

        assertFalse(v.containsTerm(TipoClinico.ENFERMEDAD, "Otra cosa"));
        assertFalse(v.containsTerm(TipoClinico.SINTOMA, "Insuficiencia cardíaca"));
        assertFalse(v.containsTerm(TipoClinico.ENFERMEDAD, null));
        assertFalse(v.containsTerm(TipoClinico.ENFERMEDAD, "   "));
    }

    @Test
    @DisplayName("allValoresFor devuelve Set con valores canónicos (no en minúsculas)")
    void allValoresForReturnsCanonicalSet() {
        Map<TipoClinico, List<VocabularyTerm>> input = new EnumMap<>(TipoClinico.class);
        input.put(TipoClinico.MEDICAMENTO, List.of(
                term(TipoClinico.MEDICAMENTO, "Losartán"),
                term(TipoClinico.MEDICAMENTO, "Atenolol")));
        Vocabulary v = new Vocabulary(ID, "cardiologia", "v1", input);

        Set<String> valores = v.allValoresFor(TipoClinico.MEDICAMENTO);
        assertEquals(2, valores.size());
        assertTrue(valores.contains("Losartán"));
        assertTrue(valores.contains("Atenolol"));
    }

    @Test
    @DisplayName("allValoresFor en bucket vacío devuelve Set vacío")
    void allValoresForEmptyBucket() {
        Vocabulary v = Vocabulary.empty(ID, "x");
        assertTrue(v.allValoresFor(TipoClinico.ENFERMEDAD).isEmpty());
    }

    @Test
    @DisplayName("Mutar input map o list después del constructor no afecta el aggregate")
    void defensiveCopies() {
        Map<TipoClinico, List<VocabularyTerm>> input = new HashMap<>();
        List<VocabularyTerm> mutableList = new ArrayList<>();
        mutableList.add(term(TipoClinico.ENFERMEDAD, "Original"));
        input.put(TipoClinico.ENFERMEDAD, mutableList);

        Vocabulary v = new Vocabulary(ID, "cardiologia", "v1", input);

        mutableList.add(term(TipoClinico.ENFERMEDAD, "Intruso"));
        input.put(TipoClinico.SINTOMA, List.of(term(TipoClinico.SINTOMA, "Otro")));

        assertEquals(1, v.getTermsFor(TipoClinico.ENFERMEDAD).size());
        assertTrue(v.getTermsFor(TipoClinico.SINTOMA).isEmpty());
    }

    @Test
    @DisplayName("Las listas devueltas son inmutables")
    void returnedListsAreImmutable() {
        Vocabulary v = Vocabulary.empty(ID, "x");
        assertThrows(UnsupportedOperationException.class,
                () -> v.getTermsFor(TipoClinico.ENFERMEDAD).add(term(TipoClinico.ENFERMEDAD, "X")));
    }
}
