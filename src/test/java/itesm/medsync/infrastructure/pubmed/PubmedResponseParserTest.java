package itesm.medsync.infrastructure.pubmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PubmedResponseParserTest {

    private PubmedResponseParser parser;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        parser = new PubmedResponseParser(objectMapper);
    }

    @Test
    @DisplayName("Parseo de JSON de esearch extrae PMIDs correctamente")
    void parseEsearchIdsOk() {
        String json = """
                {
                  "esearchresult": {
                    "idlist": ["38713028", "38713029"]
                  }
                }""";

        List<String> pmids = parser.parseEsearchIds(json);

        assertNotNull(pmids);
        assertEquals(2, pmids.size());
        assertTrue(pmids.contains("38713028"));
        assertTrue(pmids.contains("38713029"));
    }

    @Test
    @DisplayName("Parseo de JSON inválido lanza PubmedParseException")
    void parseEsearchIdsError() {
        String invalidJson = "{\"esearchresult\": {";

        assertThrows(PubmedParseException.class, () -> parser.parseEsearchIds(invalidJson));
    }

    @Test
    @DisplayName("Parseo de XML de efetch con fillna aplicado extrae datos correctamente")
    void parseEfetchArticlesOk() {
        String xml = """
                <?xml version="1.0" ?>
                <!DOCTYPE PubmedArticleSet PUBLIC "-//NLM//DTD PubMedArticle, 1st January 2024//EN" "https://dtd.nlm.nih.gov/ncbi/pubmed/out/pubmed_240101.dtd">
                <PubmedArticleSet>
                  <PubmedArticle>
                    <MedlineCitation Status="MEDLINE" Owner="NLM">
                      <PMID Version="1">12345</PMID>
                      <Article PubModel="Print">
                        <Journal>
                          <JournalIssue CitedMedium="Print">
                            <PubDate>
                              <Year>2024</Year>
                              <Month>May</Month>
                            </PubDate>
                          </JournalIssue>
                          <Title>Journal of Testing</Title>
                        </Journal>
                        <ArticleTitle>This is a test article</ArticleTitle>
                        <AuthorList>
                          <Author>
                            <LastName>Smith</LastName>
                            <ForeName>John</ForeName>
                          </Author>
                        </AuthorList>
                        <Abstract>
                          <AbstractText>Test abstract.</AbstractText>
                        </Abstract>
                        <PublicationTypeList>
                          <PublicationType UI="D016428">Journal Article</PublicationType>
                        </PublicationTypeList>
                      </Article>
                      <MeshHeadingList>
                        <MeshHeading>
                          <DescriptorName UI="D0000">TestMesh</DescriptorName>
                        </MeshHeading>
                      </MeshHeadingList>
                      <KeywordList>
                        <Keyword>TestKw</Keyword>
                      </KeywordList>
                    </MedlineCitation>
                    <PubmedData>
                      <ArticleIdList>
                        <ArticleId IdType="pubmed">12345</ArticleId>
                        <ArticleId IdType="doi">10.1234/test</ArticleId>
                      </ArticleIdList>
                    </PubmedData>
                  </PubmedArticle>
                </PubmedArticleSet>""";

        List<PubmedArticleData> articles = parser.parseEfetchArticles(xml);

        assertNotNull(articles);
        assertEquals(1, articles.size());

        PubmedArticleData data = articles.get(0);
        assertEquals("12345", data.pmid());
        assertEquals("This is a test article", data.titulo());
        assertEquals("Smith John", data.autores());
        assertEquals("Journal of Testing", data.revista());
        assertEquals(2024, data.anioPub());
        assertEquals("May", data.mesPub());
        assertEquals("10.1234/test", data.doi());
        assertEquals("Test abstract.", data.abstractText());
        assertEquals("TestMesh", data.meshTerms());
        assertEquals("TestKw", data.keywords());
        assertEquals("Journal Article", data.tipoPublicacion());
        assertEquals("https://pubmed.ncbi.nlm.nih.gov/12345/", data.url());
    }

    @Test
    @DisplayName("Parseo de XML aplica fillna 'No disponible' en campos faltantes")
    void parseEfetchArticlesFillna() {
        String xml = """
                <?xml version="1.0" ?>
                <PubmedArticleSet>
                  <PubmedArticle>
                    <MedlineCitation>
                      <PMID>67890</PMID>
                      <Article>
                        <Journal>
                          <JournalIssue></JournalIssue>
                        </Journal>
                      </Article>
                    </MedlineCitation>
                  </PubmedArticle>
                </PubmedArticleSet>""";

        List<PubmedArticleData> articles = parser.parseEfetchArticles(xml);

        assertNotNull(articles);
        assertEquals(1, articles.size());

        PubmedArticleData data = articles.get(0);
        assertEquals("No disponible", data.titulo());
        assertEquals("No disponible", data.autores());
        assertEquals("No disponible", data.revista());
        assertNull(data.anioPub());
        assertEquals("No disponible", data.mesPub());
        assertNull(data.doi()); // DOI is mapped as null if missing to avoid constraints issues
        assertEquals("No disponible", data.abstractText());
        assertEquals("No disponible", data.meshTerms());
        assertEquals("No disponible", data.keywords());
        assertEquals("No disponible", data.tipoPublicacion()); // Actually tipoPublicacion could be null or 'No disponible' based on implementation, let's check
    }

    @Test
    @DisplayName("XML Inválido arroja PubmedParseException")
    void parseEfetchArticlesError() {
        String invalidXml = "<PubmedArticleSet><PubmedArticle>...";

        assertThrows(PubmedParseException.class, () -> parser.parseEfetchArticles(invalidXml));
    }
}
