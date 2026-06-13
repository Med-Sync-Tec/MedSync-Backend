package itesm.medsync.application.article;

import itesm.medsync.domain.article.exception.ArticleNotFoundException;
import itesm.medsync.domain.article.exception.ArticleTagNotFoundException;
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
import itesm.medsync.domain.article.usecase.CreateArticleCommand;

@ExtendWith(MockitoExtension.class)
class RemoveTagFromArticleServiceTest {

    @Mock
    ArticleRepository repository;

    @InjectMocks
    RemoveTagFromArticleService service;

    @Test
    @DisplayName("Happy path: quita el tag y guarda")
    void removeOk() {
        ArticleTag tag = ArticleTag.create(TipoClinico.ENFERMEDAD, "HTA");
        Article article = Article.create(new CreateArticleCommand("titulo", null, null, 2024, null,
                null, null, null, null, null)).withTagAdded(tag);
        UUID articleId = article.getId();
        when(repository.findByUuid(articleId)).thenReturn(Optional.of(article));
        when(repository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        service.execute(articleId, tag.getId());

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(repository).save(captor.capture());
        assertEquals(0, captor.getValue().getTags().size());
    }

    @Test
    @DisplayName("Article inexistente: ArticleNotFoundException, no guarda")
    void removeArticleNotFound() {
        UUID articleId = UUID.randomUUID();
        when(repository.findByUuid(articleId)).thenReturn(Optional.empty());

        assertThrows(ArticleNotFoundException.class,
                () -> service.execute(articleId, UUID.randomUUID()));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Tag no pertenece al article: ArticleTagNotFoundException, no guarda")
    void removeTagNotFound() {
        Article article = Article.create(new CreateArticleCommand("titulo", null, null, 2024, null,
                null, null, null, null, null));
        UUID articleId = article.getId();
        when(repository.findByUuid(articleId)).thenReturn(Optional.of(article));

        assertThrows(ArticleTagNotFoundException.class,
                () -> service.execute(articleId, UUID.randomUUID()));
        verify(repository, never()).save(any());
    }
}
