package itesm.medsync.application.article;

import itesm.medsync.domain.article.exception.ArticleNotFoundException;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetArticleByIdServiceTest {

    @Mock
    ArticleRepository repository;

    @InjectMocks
    GetArticleByIdService service;

    @Test
    @DisplayName("Existe: devuelve el artículo")
    void getOk() {
        UUID id = UUID.randomUUID();
        Article article = Article.create("titulo", null, null, 2024, null,
                null, null, null, null, null);
        when(repository.findByUuid(id)).thenReturn(Optional.of(article));

        Article result = service.execute(id);
        assertEquals(article, result);
    }

    @Test
    @DisplayName("Inexistente: lanza ArticleNotFoundException")
    void getNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.empty());

        assertThrows(ArticleNotFoundException.class, () -> service.execute(id));
    }
}
