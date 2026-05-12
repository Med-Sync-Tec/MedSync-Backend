package itesm.medsync.application.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.infrastructure.pubmed.PubmedArticleData;
import itesm.medsync.infrastructure.pubmed.PubmedEutilsClient;
import itesm.medsync.infrastructure.pubmed.PubmedResponseParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncPubmedArticlesServiceTest {

    @Mock
    PubmedEutilsClient pubmedClient;

    @Mock
    PubmedResponseParser parser;

    @Mock
    ArticleRepository articleRepository;

    @InjectMocks
    SyncPubmedArticlesService service;

    @Test
    @DisplayName("Flujo exitoso: obtiene PMIDs, recupera datos y guarda artículos nuevos")
    void executeOk() {
        // Mock esearch
        String esearchJson = "{\"esearchresult\": {\"idlist\": [\"123\", \"456\"]}}";
        when(pubmedClient.searchIds(anyString(), anyString(), anyInt(), anyString())).thenReturn(esearchJson);
        when(parser.parseEsearchIds(esearchJson)).thenReturn(List.of("123", "456"));

        // Mock batch config
        when(parser.getBatchSize()).thenReturn(200);

        // Mock efetch
        String efetchXml = "<PubmedArticleSet>...</PubmedArticleSet>";
        when(pubmedClient.fetchArticles(anyString(), eq("123,456"), anyString(), anyString())).thenReturn(efetchXml);
        
        PubmedArticleData article1 = new PubmedArticleData("123", "Title 1", "Auth 1", "Journal", 2024, "Jan", "10.1/123", "Abstract", "Mesh", "Kw", "Journal Article", "url1");
        PubmedArticleData article2 = new PubmedArticleData("456", "Title 2", "Auth 2", "Journal", 2024, "Feb", null, "Abstract", "Mesh", "Kw", "Journal Article", "url2");
        when(parser.parseEfetchArticles(efetchXml)).thenReturn(List.of(article1, article2));

        // Mock repository
        when(articleRepository.existsByDoi("10.1/123")).thenReturn(false);
        // El artículo 2 tiene doi null, por lo que no debe llamar a existsByDoi
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

        int result = service.execute();

        assertEquals(2, result);
        verify(articleRepository, times(2)).save(any(Article.class));
    }

    @Test
    @DisplayName("Sin PMIDs: no invoca a efetch ni al repositorio")
    void executeNoPmids() {
        String esearchJson = "{\"esearchresult\": {\"idlist\": []}}";
        when(pubmedClient.searchIds(anyString(), anyString(), anyInt(), anyString())).thenReturn(esearchJson);
        when(parser.parseEsearchIds(esearchJson)).thenReturn(Collections.emptyList());

        int result = service.execute();

        assertEquals(0, result);
        verify(pubmedClient, never()).fetchArticles(anyString(), anyString(), anyString(), anyString());
        verify(articleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Error en efetch: captura excepción y continúa o retorna 0 si falla el único lote")
    void executeEfetchError() {
        String esearchJson = "{\"esearchresult\": {\"idlist\": [\"123\"]}}";
        when(pubmedClient.searchIds(anyString(), anyString(), anyInt(), anyString())).thenReturn(esearchJson);
        when(parser.parseEsearchIds(esearchJson)).thenReturn(List.of("123"));
        when(parser.getBatchSize()).thenReturn(200);

        // Simulate efetch exception
        when(pubmedClient.fetchArticles(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API Error"));

        int result = service.execute();

        assertEquals(0, result);
        verify(articleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Artículo duplicado por DOI es ignorado")
    void executeDoiAlreadyExists() {
        String esearchJson = "{\"esearchresult\": {\"idlist\": [\"123\"]}}";
        when(pubmedClient.searchIds(anyString(), anyString(), anyInt(), anyString())).thenReturn(esearchJson);
        when(parser.parseEsearchIds(esearchJson)).thenReturn(List.of("123"));
        when(parser.getBatchSize()).thenReturn(200);

        String efetchXml = "<PubmedArticleSet>...</PubmedArticleSet>";
        when(pubmedClient.fetchArticles(anyString(), anyString(), anyString(), anyString())).thenReturn(efetchXml);
        
        PubmedArticleData article1 = new PubmedArticleData("123", "Title 1", "Auth 1", "Journal", 2024, "Jan", "10.1/123", "Abstract", "Mesh", "Kw", "Journal Article", "url1");
        when(parser.parseEfetchArticles(efetchXml)).thenReturn(List.of(article1));

        // Mock existsByDoi -> true
        when(articleRepository.existsByDoi("10.1/123")).thenReturn(true);

        int result = service.execute();

        assertEquals(1, result);
        verify(articleRepository, never()).save(any());
    }
}
