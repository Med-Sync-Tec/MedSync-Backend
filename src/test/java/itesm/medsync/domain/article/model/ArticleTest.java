package itesm.medsync.domain.article.model;

import itesm.medsync.domain.article.exception.InvalidArticleDataException;
import itesm.medsync.domain.shared.model.TipoClinico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import itesm.medsync.domain.article.usecase.CreateArticleCommand;

class ArticleTest {

    private Article newValidArticle() {
        return Article.create(new CreateArticleCommand(
                "Tratamiento de hipertensión",
                "García J, López P",
                "JAMA",
                2024,
                "Mar",
                "10.1234/abc",
                "Resumen del estudio...",
                "hipertension, beta-bloqueador",
                "Journal Article",
                "https://doi.org/10.1234/abc"));
    }

    @Test
    @DisplayName("create asigna id, lista vacía de tags y timestamps nulos")
    void createOk() {
        Article article = newValidArticle();

        assertNotNull(article.getId());
        assertEquals("Tratamiento de hipertensión", article.getTitulo());
        assertEquals(2024, article.getAnioPub());
        assertEquals("10.1234/abc", article.getDoi());
        assertTrue(article.getTags().isEmpty());
        assertNull(article.getCreatedAt());
    }

    @Test
    @DisplayName("titulo blank lanza InvalidArticleDataException")
    void tituloBlankThrows() {
        assertThrows(InvalidArticleDataException.class,
                () -> Article.create(new CreateArticleCommand("   ", null, null, null, null, null, null, null, null, null)));
    }

    @Test
    @DisplayName("anioPub fuera de rango lanza InvalidArticleDataException")
    void anioFueraDeRango() {
        assertThrows(InvalidArticleDataException.class,
                () -> Article.create(new CreateArticleCommand("titulo", null, null, 1700, null, null, null, null, null, null)));
        int futuro = 2035;
        assertThrows(InvalidArticleDataException.class,
                () -> Article.create(new CreateArticleCommand("titulo", null, null, futuro, null, null, null, null, null, null)));
    }

    @Test
    @DisplayName("anioPub null es válido (campo opcional)")
    void anioNullPermitido() {
        Article a = Article.create(new CreateArticleCommand("titulo", null, null, null, null, null, null, null, null, null));
        assertNull(a.getAnioPub());
    }

    @Test
    @DisplayName("doi blank se normaliza a null")
    void doiBlankToNull() {
        Article a = Article.create(new CreateArticleCommand("titulo", null, null, null, null, "   ", null, null, null, null));
        assertNull(a.getDoi());
    }

    @Test
    @DisplayName("withTagAdded retorna nueva instancia con el tag agregado (immutabilidad)")
    void addTagImmutable() {
        Article original = newValidArticle();
        ArticleTag tag = ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión");

        Article updated = original.withTagAdded(tag);

        assertEquals(0, original.getTags().size(), "Original no debe mutar");
        assertEquals(1, updated.getTags().size());
        assertEquals(tag, updated.getTags().get(0));
        assertEquals(original.getId(), updated.getId());
    }

    @Test
    @DisplayName("withTagRemoved quita el tag por id; original no muta")
    void removeTagImmutable() {
        ArticleTag tag = ArticleTag.create(TipoClinico.SINTOMA, "Mareo");
        Article withTag = newValidArticle().withTagAdded(tag);

        Article withoutTag = withTag.withTagRemoved(tag.getId());

        assertEquals(1, withTag.getTags().size(), "Versión con tag no debe mutar");
        assertEquals(0, withoutTag.getTags().size());
    }

    @Test
    @DisplayName("withTagAdded con tag null lanza InvalidArticleDataException")
    void addNullTagThrows() {
        Article a = newValidArticle();
        assertThrows(InvalidArticleDataException.class, () -> a.withTagAdded(null));
    }

    @Test
    @DisplayName("getTags devuelve lista inmutable")
    void tagsListUnmodifiable() {
        Article a = newValidArticle().withTagAdded(ArticleTag.create(TipoClinico.ENFERMEDAD, "x"));
        assertThrows(UnsupportedOperationException.class,
                () -> a.getTags().add(ArticleTag.create(TipoClinico.SINTOMA, "y")));
    }

    @Test
    @DisplayName("withAiAnalysis reemplaza la lista de tags y asigna especialidad; no muta el original")
    void withAiAnalysisReplacesTagsAndSpecialty() {
        Article original = newValidArticle()
                .withTagAdded(ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión arterial"));
        UUID newSpecialty = UUID.randomUUID();
        ArticleTag aiTag1 = ArticleTag.create(TipoClinico.ENFERMEDAD, "Insuficiencia cardíaca");
        ArticleTag aiTag2 = ArticleTag.create(TipoClinico.MEDICAMENTO, "Losartán");

        Article analyzed = original.withAiAnalysis(newSpecialty, List.of(aiTag1, aiTag2));

        assertEquals(1, original.getTags().size(), "Original no debe mutar");
        assertNull(original.getEspecialidadId(), "Original mantiene especialidad previa (null)");
        assertEquals(newSpecialty, analyzed.getEspecialidadId());
        assertEquals(2, analyzed.getTags().size());
        assertTrue(analyzed.getTags().contains(aiTag1));
        assertTrue(analyzed.getTags().contains(aiTag2));
        assertEquals(original.getId(), analyzed.getId());
        assertEquals(original.getTitulo(), analyzed.getTitulo());
        assertEquals(original.getDoi(), analyzed.getDoi());
    }

    @Test
    @DisplayName("withAiAnalysis con lista vacía elimina todos los tags existentes")
    void withAiAnalysisEmptyReplacesAllTags() {
        Article withTags = newValidArticle()
                .withTagAdded(ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión"))
                .withTagAdded(ArticleTag.create(TipoClinico.SINTOMA, "Mareo"));
        UUID newSpecialty = UUID.randomUUID();

        Article analyzed = withTags.withAiAnalysis(newSpecialty, List.of());

        assertEquals(0, analyzed.getTags().size());
        assertEquals(newSpecialty, analyzed.getEspecialidadId());
    }

    @Test
    @DisplayName("withAiAnalysis con especialidadId null lanza InvalidArticleDataException")
    void withAiAnalysisNullSpecialtyThrows() {
        Article a = newValidArticle();
        assertThrows(InvalidArticleDataException.class,
                () -> a.withAiAnalysis(null, List.of()));
    }

    @Test
    @DisplayName("withAiAnalysis con lista de tags null lanza InvalidArticleDataException")
    void withAiAnalysisNullTagsThrows() {
        Article a = newValidArticle();
        UUID specialty = UUID.randomUUID();
        assertThrows(InvalidArticleDataException.class,
                () -> a.withAiAnalysis(specialty, null));
    }
}
