package itesm.medsync.application.article;

import itesm.medsync.domain.article.exception.DuplicateArticleException;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateArticleServiceTest {

    @Mock
    ArticleRepository repository;

    @InjectMocks
    CreateArticleService service;

    @Test
    @DisplayName("Happy path: guarda y devuelve el artículo")
    void createOk() {
        when(repository.existsByDoi("10.1234/abc")).thenReturn(false);
        when(repository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        Article result = service.execute(
                "Tratamiento", "García", "JAMA", 2024, "Mar",
                "10.1234/abc", "abstract", "kw", "Journal Article",
                "https://doi.org/10.1234/abc");

        assertNotNull(result);
        assertEquals("Tratamiento", result.getTitulo());
        assertEquals("10.1234/abc", result.getDoi());
        verify(repository).save(any(Article.class));
    }

    @Test
    @DisplayName("DOI duplicado lanza DuplicateArticleException, no guarda")
    void createDuplicate() {
        when(repository.existsByDoi("10.1234/dup")).thenReturn(true);

        DuplicateArticleException ex = assertThrows(DuplicateArticleException.class,
                () -> service.execute("titulo", null, null, null, null,
                        "10.1234/dup", null, null, null, null));
        assertEquals("10.1234/dup", ex.getDoi());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("DOI null no consulta existencia y guarda")
    void createDoiNull() {
        when(repository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        Article result = service.execute("titulo", null, null, null, null,
                null, null, null, null, null);

        assertNull(result.getDoi());
        verify(repository, never()).existsByDoi(any());
        verify(repository).save(any());
    }
}
