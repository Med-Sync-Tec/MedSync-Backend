package itesm.medsync.domain.consultaaianalysis.model;

import itesm.medsync.domain.shared.model.ExtractedTag;
import itesm.medsync.domain.shared.model.TipoClinico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConsultaAnalysisTest {

    private static final UUID SPECIALTY_ID = UUID.randomUUID();

    @Test
    @DisplayName("POPULATED con sugerencias y tokens > 0 es válido")
    void populatedHappy() {
        List<ExtractedTag> tags = List.of(
                new ExtractedTag(TipoClinico.ENFERMEDAD, "Hipertensión arterial"),
                new ExtractedTag(TipoClinico.MEDICAMENTO, "Losartán"));

        ConsultaAnalysis ca = new ConsultaAnalysis(
                "c-1", SPECIALTY_ID, "cardiologia",
                VocabularyStatus.POPULATED, tags,
                "llama-3.3-70b-versatile", 500, 80);

        assertEquals(VocabularyStatus.POPULATED, ca.vocabularyStatus());
        assertEquals(2, ca.suggestions().size());
        assertEquals(500, ca.promptTokens());
    }

    @Test
    @DisplayName("EMPTY con sugerencias vacías y tokens en 0 es válido")
    void emptyStatusOk() {
        ConsultaAnalysis ca = new ConsultaAnalysis(
                "c-1", SPECIALTY_ID, "cardiologia",
                VocabularyStatus.EMPTY, List.of(),
                "", 0, 0);

        assertEquals(VocabularyStatus.EMPTY, ca.vocabularyStatus());
        assertTrue(ca.suggestions().isEmpty());
        assertEquals(0, ca.promptTokens());
        assertEquals(0, ca.completionTokens());
    }

    @Test
    @DisplayName("EMPTY con sugerencias no vacías viola el invariante")
    void emptyWithSuggestionsRejected() {
        List<ExtractedTag> tags = List.of(new ExtractedTag(TipoClinico.ENFERMEDAD, "X"));
        assertThrows(IllegalArgumentException.class,
                () -> new ConsultaAnalysis(
                        "c-1", SPECIALTY_ID, "cardiologia",
                        VocabularyStatus.EMPTY, tags,
                        "", 0, 0));
    }

    @Test
    @DisplayName("EMPTY con tokens > 0 viola el invariante (no se llamó al LLM)")
    void emptyWithTokensRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConsultaAnalysis(
                        "c-1", SPECIALTY_ID, "cardiologia",
                        VocabularyStatus.EMPTY, List.of(),
                        "", 100, 0));
    }

    @Test
    @DisplayName("null consultaId / especialidadId / suggestions / status lanza NPE")
    void nullsRejected() {
        assertThrows(NullPointerException.class,
                () -> new ConsultaAnalysis(null, SPECIALTY_ID, "s",
                        VocabularyStatus.POPULATED, List.of(), "m", 0, 0));
        assertThrows(NullPointerException.class,
                () -> new ConsultaAnalysis("c", null, "s",
                        VocabularyStatus.POPULATED, List.of(), "m", 0, 0));
        assertThrows(NullPointerException.class,
                () -> new ConsultaAnalysis("c", SPECIALTY_ID, "s",
                        null, List.of(), "m", 0, 0));
        assertThrows(NullPointerException.class,
                () -> new ConsultaAnalysis("c", SPECIALTY_ID, "s",
                        VocabularyStatus.POPULATED, null, "m", 0, 0));
    }
}
