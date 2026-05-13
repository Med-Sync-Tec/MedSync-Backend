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
        String json = "{\n" +
                "  \"esearchresult\": {\n" +
                "    \"idlist\": [\"38713028\", \"38713029\"]\n" +
                "  }\n" +
                "}";

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
        String xml = "<?xml version=\"1.0\" ?>\n" +
                "<!DOCTYPE PubmedArticleSet PUBLIC \"-//NLM//DTD PubMedArticle, 1st January 2024//EN\" \"https://dtd.nlm.nih.gov/ncbi/pubmed/out/pubmed_240101.dtd\">\n" +
                "<PubmedArticleSet>\n" +
                "  <PubmedArticle>\n" +
                "    <MedlineCitation Status=\"MEDLINE\" Owner=\"NLM\">\n" +
                "      <PMID Version=\"1\">12345</PMID>\n" +
                "      <Article PubModel=\"Print\">\n" +
                "        <Journal>\n" +
                "          <JournalIssue CitedMedium=\"Print\">\n" +
                "            <PubDate>\n" +
                "              <Year>2024</Year>\n" +
                "              <Month>May</Month>\n" +
                "            </PubDate>\n" +
                "          </JournalIssue>\n" +
                "          <Title>Journal of Testing</Title>\n" +
                "        </Journal>\n" +
                "        <ArticleTitle>This is a test article</ArticleTitle>\n" +
                "        <AuthorList>\n" +
                "          <Author>\n" +
                "            <LastName>Smith</LastName>\n" +
                "            <ForeName>John</ForeName>\n" +
                "          </Author>\n" +
                "        </AuthorList>\n" +
                "        <Abstract>\n" +
                "          <AbstractText>Test abstract.</AbstractText>\n" +
                "        </Abstract>\n" +
                "        <PublicationTypeList>\n" +
                "          <PublicationType UI=\"D016428\">Journal Article</PublicationType>\n" +
                "        </PublicationTypeList>\n" +
                "      </Article>\n" +
                "      <MeshHeadingList>\n" +
                "        <MeshHeading>\n" +
                "          <DescriptorName UI=\"D0000\">TestMesh</DescriptorName>\n" +
                "        </MeshHeading>\n" +
                "      </MeshHeadingList>\n" +
                "      <KeywordList>\n" +
                "        <Keyword>TestKw</Keyword>\n" +
                "      </KeywordList>\n" +
                "    </MedlineCitation>\n" +
                "    <PubmedData>\n" +
                "      <ArticleIdList>\n" +
                "        <ArticleId IdType=\"pubmed\">12345</ArticleId>\n" +
                "        <ArticleId IdType=\"doi\">10.1234/test</ArticleId>\n" +
                "      </ArticleIdList>\n" +
                "    </PubmedData>\n" +
                "  </PubmedArticle>\n" +
                "</PubmedArticleSet>";

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
        String xml = "<?xml version=\"1.0\" ?>\n" +
                "<PubmedArticleSet>\n" +
                "  <PubmedArticle>\n" +
                "    <MedlineCitation>\n" +
                "      <PMID>67890</PMID>\n" +
                "      <Article>\n" +
                "        <Journal>\n" +
                "          <JournalIssue></JournalIssue>\n" +
                "        </Journal>\n" +
                "      </Article>\n" +
                "    </MedlineCitation>\n" +
                "  </PubmedArticle>\n" +
                "</PubmedArticleSet>";

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
