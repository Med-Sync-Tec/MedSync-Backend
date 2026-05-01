package itesm.medsync.domain.article.model;

import itesm.medsync.domain.article.exception.InvalidArticleDataException;
import itesm.medsync.domain.shared.model.TipoClinico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.*;

class ArticleTest {

    private Article newValidArticle() {
        return Article.create(
                "Tratamiento de hipertensión",
                "García J, López P",
                "JAMA",
                2024,
                "Mar",
                "10.1234/abc",
                "Resumen del estudio...",
                "hipertension, beta-bloqueador",
                "Journal Article",
                "https://doi.org/10.1234/abc");
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
                () -> Article.create("   ", null, null, null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("anioPub fuera de rango lanza InvalidArticleDataException")
    void anioFueraDeRango() {
        assertThrows(InvalidArticleDataException.class,
                () -> Article.create("titulo", null, null, 1700, null, null, null, null, null, null));
        int futuro = Year.now().getValue() + 5;
        assertThrows(InvalidArticleDataException.class,
                () -> Article.create("titulo", null, null, futuro, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("anioPub null es válido (campo opcional)")
    void anioNullPermitido() {
        Article a = Article.create("titulo", null, null, null, null, null, null, null, null, null);
        assertNull(a.getAnioPub());
    }

    @Test
    @DisplayName("doi blank se normaliza a null")
    void doiBlankToNull() {
        Article a = Article.create("titulo", null, null, null, null, "   ", null, null, null, null);
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
}
