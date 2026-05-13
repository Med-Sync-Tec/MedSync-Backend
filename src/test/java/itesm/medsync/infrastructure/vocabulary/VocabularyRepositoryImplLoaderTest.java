package itesm.medsync.infrastructure.vocabulary;

import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.vocabulary.exception.VocabularyParseException;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VocabularyRepositoryImplLoaderTest {

    private final VocabularyJsonReader reader = new VocabularyJsonReader();

    private VocabularyLoader.RawFile rawFile(String slug, String json) {
        return new VocabularyLoader.RawFile(slug, json.getBytes(StandardCharsets.UTF_8));
    }

    private String validJson(String slug, String term) {
        return """
                {
                  "especialidadSlug": "%s",
                  "version": "v1",
                  "terms": { "ENFERMEDAD": ["%s"] }
                }
                """.formatted(slug, term);
    }

    @Test
    @DisplayName("3 archivos válidos → mapa con 3 entradas, conteos correctos")
    void allValid() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        UUID idC = UUID.randomUUID();
        Map<String, UUID> slugToId = Map.of("a", idA, "b", idB, "c", idC);
        VocabularyLoader loader = new VocabularyLoader(reader);

        VocabularyLoader.BuildResult result = loader.build(slugToId, List.of(
                rawFile("a", validJson("a", "TermA")),
                rawFile("b", validJson("b", "TermB")),
                rawFile("c", validJson("c", "TermC"))
        ));

        assertEquals(3, result.loaded().size());
        assertEquals(1, result.loaded().get(idA).totalTerms());
        assertEquals(0, result.staleSlugs().size());
        assertTrue(result.loaded().get(idA).containsTerm(TipoClinico.ENFERMEDAD, "TermA"));
    }

    @Test
    @DisplayName("Archivo cuyo slug no matchea ninguna especialidad → excluido + staleSlugs lista el slug")
    void slugWithoutSpecialty() {
        UUID idA = UUID.randomUUID();
        Map<String, UUID> slugToId = Map.of("a", idA);
        VocabularyLoader loader = new VocabularyLoader(reader);

        VocabularyLoader.BuildResult result = loader.build(slugToId, List.of(
                rawFile("a", validJson("a", "X")),
                rawFile("zombie", validJson("zombie", "Y"))
        ));

        assertEquals(1, result.loaded().size());
        assertTrue(result.loaded().containsKey(idA));
        assertEquals(List.of("zombie"), result.staleSlugs());
    }

    @Test
    @DisplayName("Especialidad sin archivo → no aparece en el mapa; getVocabularyFor devuelve empty tras installMap")
    void specialtyWithoutFile() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        Map<String, UUID> slugToId = Map.of("a", idA, "b", idB);
        VocabularyLoader loader = new VocabularyLoader(reader);

        VocabularyLoader.BuildResult result = loader.build(slugToId, List.of(
                rawFile("a", validJson("a", "X"))
        ));

        VocabularyRepositoryImpl repo = new VocabularyRepositoryImpl();
        repo.installMap(result.loaded());

        assertFalse(result.loaded().containsKey(idB));
        Vocabulary empty = repo.getVocabularyFor(idB);
        assertEquals("empty", empty.getVersion());
        assertEquals(0, empty.totalTerms());

        assertEquals(1, repo.getVocabularyFor(idA).totalTerms());
    }

    @Test
    @DisplayName("Archivo malformado → VocabularyParseException propagada desde build(...)")
    void malformedFilePropagates() {
        UUID idA = UUID.randomUUID();
        Map<String, UUID> slugToId = Map.of("a", idA);
        VocabularyLoader loader = new VocabularyLoader(reader);

        List<VocabularyLoader.RawFile> files = List.of(
                rawFile("a", "{ this is not json")
        );

        assertThrows(VocabularyParseException.class, () -> loader.build(slugToId, files));
    }

    @Test
    @DisplayName("Antes de installMap, getVocabularyFor devuelve Vocabulary.empty(...) (no NPE)")
    void readBeforeInstallReturnsEmpty() {
        VocabularyRepositoryImpl repo = new VocabularyRepositoryImpl();
        UUID id = UUID.randomUUID();
        Vocabulary v = repo.getVocabularyFor(id);
        assertEquals("empty", v.getVersion());
    }
}
