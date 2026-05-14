package itesm.medsync.infrastructure.vocabulary;

import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.vocabulary.exception.VocabularyParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class VocabularyJsonReaderTest {

    private final VocabularyJsonReader reader = new VocabularyJsonReader();

    private byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("JSON válido → ParsedVocabularyFile con los 4 buckets poblados")
    void happyPath() {
        String json = """
                {
                  "especialidadSlug": "cardiologia",
                  "version": "2026-05-12",
                  "terms": {
                    "ENFERMEDAD": ["Hipertensión", "Infarto agudo"],
                    "SINTOMA":    ["Disnea"],
                    "TRATAMIENTO":["Angioplastia"],
                    "MEDICAMENTO":["Losartán"]
                  }
                }
                """;

        ParsedVocabularyFile parsed = reader.parse("cardiologia", bytes(json));

        assertEquals("cardiologia", parsed.slug());
        assertEquals("2026-05-12", parsed.version());
        assertEquals(2, parsed.termsByType().get(TipoClinico.ENFERMEDAD).size());
        assertEquals("Hipertensión", parsed.termsByType().get(TipoClinico.ENFERMEDAD).get(0));
        assertEquals(1, parsed.termsByType().get(TipoClinico.SINTOMA).size());
        assertEquals(1, parsed.termsByType().get(TipoClinico.TRATAMIENTO).size());
        assertEquals(1, parsed.termsByType().get(TipoClinico.MEDICAMENTO).size());
    }

    @Test
    @DisplayName("$schema válido es aceptado")
    void schemaFieldAccepted() {
        String json = """
                {
                  "$schema": "medsync.vocabulary.v1",
                  "especialidadSlug": "cardiologia",
                  "version": "v1",
                  "terms": { "ENFERMEDAD": ["X"] }
                }
                """;
        assertDoesNotThrow(() -> reader.parse("cardiologia", bytes(json)));
    }

    @Test
    @DisplayName("$schema con valor distinto a 'medsync.vocabulary.v1' → exception")
    void schemaFieldRejected() {
        String json = """
                {
                  "$schema": "medsync.vocabulary.v999",
                  "especialidadSlug": "cardiologia",
                  "version": "v1",
                  "terms": { "ENFERMEDAD": ["X"] }
                }
                """;
        VocabularyParseException ex = assertThrows(VocabularyParseException.class,
                () -> reader.parse("cardiologia", bytes(json)));
        assertTrue(ex.getMessage().contains("$schema"));
    }

    @Test
    @DisplayName("especialidadSlug ausente → exception nombrando el campo")
    void missingSlug() {
        String json = """
                { "version": "v1", "terms": {} }
                """;
        VocabularyParseException ex = assertThrows(VocabularyParseException.class,
                () -> reader.parse("cardiologia", bytes(json)));
        assertTrue(ex.getMessage().contains("especialidadSlug"));
    }

    @Test
    @DisplayName("especialidadSlug ≠ filename → exception nombrando ambos")
    void slugMismatch() {
        String json = """
                { "especialidadSlug": "neurologia", "version": "v1", "terms": {} }
                """;
        VocabularyParseException ex = assertThrows(VocabularyParseException.class,
                () -> reader.parse("cardiologia", bytes(json)));
        assertTrue(ex.getMessage().contains("cardiologia"));
        assertTrue(ex.getMessage().contains("neurologia"));
    }

    @Test
    @DisplayName("version ausente → exception")
    void missingVersion() {
        String json = """
                { "especialidadSlug": "cardiologia", "terms": {} }
                """;
        VocabularyParseException ex = assertThrows(VocabularyParseException.class,
                () -> reader.parse("cardiologia", bytes(json)));
        assertTrue(ex.getMessage().contains("version"));
    }

    @Test
    @DisplayName("terms ausente → exception")
    void missingTerms() {
        String json = """
                { "especialidadSlug": "cardiologia", "version": "v1" }
                """;
        VocabularyParseException ex = assertThrows(VocabularyParseException.class,
                () -> reader.parse("cardiologia", bytes(json)));
        assertTrue(ex.getMessage().contains("terms"));
    }

    @Test
    @DisplayName("Unknown TipoClinico key → exception nombrando la key")
    void unknownTipoKey() {
        String json = """
                {
                  "especialidadSlug": "cardiologia",
                  "version": "v1",
                  "terms": { "ALERGIA": ["X"] }
                }
                """;
        VocabularyParseException ex = assertThrows(VocabularyParseException.class,
                () -> reader.parse("cardiologia", bytes(json)));
        assertTrue(ex.getMessage().contains("ALERGIA"));
    }

    @Test
    @DisplayName("Término vacío o whitespace-only → exception nombrando bucket y posición")
    void blankTerm() {
        String json = """
                {
                  "especialidadSlug": "cardiologia",
                  "version": "v1",
                  "terms": { "ENFERMEDAD": ["Hipertensión", "   "] }
                }
                """;
        VocabularyParseException ex = assertThrows(VocabularyParseException.class,
                () -> reader.parse("cardiologia", bytes(json)));
        assertTrue(ex.getMessage().contains("ENFERMEDAD"));
    }

    @Test
    @DisplayName("Término > 500 chars → exception")
    void termTooLong() {
        String longTerm = "a".repeat(501);
        String json = """
                {
                  "especialidadSlug": "cardiologia",
                  "version": "v1",
                  "terms": { "ENFERMEDAD": ["%s"] }
                }
                """.formatted(longTerm);
        VocabularyParseException ex = assertThrows(VocabularyParseException.class,
                () -> reader.parse("cardiologia", bytes(json)));
        assertTrue(ex.getMessage().contains("ENFERMEDAD"));
    }

    @Test
    @DisplayName("Duplicado case-insensitive dentro del mismo bucket → exception listando ambos")
    void caseInsensitiveDuplicate() {
        String json = """
                {
                  "especialidadSlug": "cardiologia",
                  "version": "v1",
                  "terms": { "ENFERMEDAD": ["Hipertensión", "  HIPERTENSIÓN  "] }
                }
                """;
        VocabularyParseException ex = assertThrows(VocabularyParseException.class,
                () -> reader.parse("cardiologia", bytes(json)));
        assertTrue(ex.getMessage().toLowerCase().contains("duplicat"));
        assertTrue(ex.getMessage().contains("Hipertensión"));
    }

    @Test
    @DisplayName("Bucket vacío ([]) → sin exception, bucket vacío")
    void emptyBucket() {
        String json = """
                {
                  "especialidadSlug": "cardiologia",
                  "version": "v1",
                  "terms": { "ENFERMEDAD": [], "SINTOMA": ["X"] }
                }
                """;
        ParsedVocabularyFile parsed = reader.parse("cardiologia", bytes(json));
        assertTrue(parsed.termsByType().get(TipoClinico.ENFERMEDAD).isEmpty());
    }

    @Test
    @DisplayName("Bucket omitido (sin la key) → sin exception, bucket vacío")
    void omittedBucket() {
        String json = """
                {
                  "especialidadSlug": "cardiologia",
                  "version": "v1",
                  "terms": { "ENFERMEDAD": ["X"] }
                }
                """;
        ParsedVocabularyFile parsed = reader.parse("cardiologia", bytes(json));
        assertTrue(parsed.termsByType().get(TipoClinico.SINTOMA).isEmpty());
        assertTrue(parsed.termsByType().get(TipoClinico.TRATAMIENTO).isEmpty());
        assertTrue(parsed.termsByType().get(TipoClinico.MEDICAMENTO).isEmpty());
    }

    @Test
    @DisplayName("JSON truncado → VocabularyParseException envolviendo el error de Jackson")
    void malformedJson() {
        String json = """
                { "especialidadSlug": "cardiologia", "version": "v1", "terms":
                """;
        VocabularyParseException ex = assertThrows(VocabularyParseException.class,
                () -> reader.parse("cardiologia", bytes(json)));
        assertNotNull(ex.getCause());
    }
}
