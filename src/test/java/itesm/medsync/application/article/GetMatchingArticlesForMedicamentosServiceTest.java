package itesm.medsync.application.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetMatchingArticlesForMedicamentosServiceTest {

    @Mock
    ArticleRepository articleRepository;

    @InjectMocks
    GetMatchingArticlesForMedicamentosService service;

    @Test
    @DisplayName("Happy path: delega al repository con el límite recibido")
    void matchOk() {
        Article article = Article.create("titulo", null, null, 2024, null,
                null, null, null, null, null);
        when(articleRepository.findMatchingArticlesForMedicamentos(anyInt()))
                .thenReturn(List.of(article));

        List<Article> result = service.execute(50);

        assertEquals(1, result.size());
        verify(articleRepository).findMatchingArticlesForMedicamentos(50);
    }

    @Test
    @DisplayName("Límite no positivo cae al valor por defecto (50)")
    void clampsNonPositiveToDefault() {
        when(articleRepository.findMatchingArticlesForMedicamentos(anyInt()))
                .thenReturn(List.of());

        service.execute(0);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(articleRepository).findMatchingArticlesForMedicamentos(captor.capture());
        assertEquals(50, captor.getValue());
    }

    @Test
    @DisplayName("Límite excesivo se acota al máximo (100)")
    void clampsToMax() {
        when(articleRepository.findMatchingArticlesForMedicamentos(anyInt()))
                .thenReturn(List.of());

        service.execute(9999);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(articleRepository).findMatchingArticlesForMedicamentos(captor.capture());
        assertEquals(100, captor.getValue());
    }
}
