package itesm.medsync.application.article;

import itesm.medsync.domain.article.exception.ArticleNotFoundException;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.shared.model.TipoClinico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddTagToArticleServiceTest {

    @Mock
    ArticleRepository repository;

    @InjectMocks
    AddTagToArticleService service;

    @Test
    @DisplayName("Happy path: agrega tag y guarda; el resultado está en el saved")
    void addOk() {
        UUID articleId = UUID.randomUUID();
        Article article = Article.create("titulo", null, null, 2024, null,
                null, null, null, null, null);
        when(repository.findByUuid(articleId)).thenReturn(Optional.of(article));
        when(repository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        ArticleTag tag = service.execute(articleId, TipoClinico.ENFERMEDAD, "Hipertensión");

        assertNotNull(tag);
        assertEquals(TipoClinico.ENFERMEDAD, tag.getTipo());
        assertEquals("Hipertensión", tag.getValor());

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(repository).save(captor.capture());
        assertEquals(1, captor.getValue().getTags().size());
    }

    @Test
    @DisplayName("Article inexistente: ArticleNotFoundException, no guarda")
    void addNotFound() {
        UUID articleId = UUID.randomUUID();
        when(repository.findByUuid(articleId)).thenReturn(Optional.empty());

        assertThrows(ArticleNotFoundException.class,
                () -> service.execute(articleId, TipoClinico.SINTOMA, "x"));
        verify(repository, never()).save(any());
    }
}
