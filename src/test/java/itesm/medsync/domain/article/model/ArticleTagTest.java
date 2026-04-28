package itesm.medsync.domain.article.model;

import itesm.medsync.domain.article.exception.InvalidArticleDataException;
import itesm.medsync.domain.shared.model.TipoClinico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArticleTagTest {

    @Test
    @DisplayName("create asigna id y deja createdAt nulo")
    void createOk() {
        ArticleTag tag = ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión");

        assertNotNull(tag.getId());
        assertEquals(TipoClinico.ENFERMEDAD, tag.getTipo());
        assertEquals("Hipertensión", tag.getValor());
        assertNull(tag.getCreatedAt());
    }

    @Test
    @DisplayName("create con valor con espacios lo trimea")
    void createTrimsValor() {
        ArticleTag tag = ArticleTag.create(TipoClinico.SINTOMA, "  Mareo  ");
        assertEquals("Mareo", tag.getValor());
    }

    @Test
    @DisplayName("tipo null lanza InvalidArticleDataException")
    void tipoNullThrows() {
        assertThrows(InvalidArticleDataException.class,
                () -> ArticleTag.create(null, "x"));
    }

    @Test
    @DisplayName("valor blank lanza InvalidArticleDataException")
    void valorBlankThrows() {
        assertThrows(InvalidArticleDataException.class,
                () -> ArticleTag.create(TipoClinico.ENFERMEDAD, "   "));
    }

    @Test
    @DisplayName("valor mayor a 500 chars lanza InvalidArticleDataException")
    void valorTooLongThrows() {
        String tooLong = "a".repeat(501);
        assertThrows(InvalidArticleDataException.class,
                () -> ArticleTag.create(TipoClinico.ENFERMEDAD, tooLong));
    }
}
