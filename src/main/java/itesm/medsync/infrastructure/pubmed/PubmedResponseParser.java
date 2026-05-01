package itesm.medsync.infrastructure.pubmed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser para las respuestas de la API E-utilities de NCBI.
 * <p>
 * - {@link #parseEsearchIds(String)}: extrae los PMIDs del JSON de esearch.
 * - {@link #parseEfetchArticles(String)}: parsea el XML de efetch y devuelve
 *   una lista de {@link PubmedArticleData} con limpieza de datos (fillna).
 */
@ApplicationScoped
public class PubmedResponseParser {

    private static final String NO_DISPONIBLE = "No disponible";
    private static final int BATCH_SIZE = 200;

    private final ObjectMapper objectMapper;

    @Inject
    public PubmedResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Extrae la lista de PMIDs desde el JSON de respuesta de esearch.
     */
    public List<String> parseEsearchIds(String esearchJson) {
        List<String> ids = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(esearchJson);
            JsonNode idList = root.path("esearchresult").path("idlist");
            if (idList.isArray()) {
                for (JsonNode node : idList) {
                    ids.add(node.asText());
                }
            }
        } catch (Exception e) {
            throw new PubmedParseException("Error parseando esearch JSON: " + e.getMessage(), e);
        }
        return ids;
    }

    /**
     * Devuelve el tamaño de lote recomendado para llamadas a efetch.
     */
    public int getBatchSize() {
        return BATCH_SIZE;
    }

    /**
     * Parsea el XML de efetch y aplica limpieza (fillna) a los campos nulos.
     */
    public List<PubmedArticleData> parseEfetchArticles(String efetchXml) {
        List<PubmedArticleData> result = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // PubMed envía DOCTYPE en su XML, por lo que debemos permitirlo (false).
            // Sin embargo, desactivamos entidades externas para mantener la seguridad (XXE).
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(
                    new ByteArrayInputStream(efetchXml.getBytes(StandardCharsets.UTF_8)));

            NodeList articles = doc.getElementsByTagName("PubmedArticle");
            for (int i = 0; i < articles.getLength(); i++) {
                Element article = (Element) articles.item(i);
                PubmedArticleData data = extractArticle(article);
                result.add(data);
            }
        } catch (Exception e) {
            throw new PubmedParseException("Error parseando efetch XML: " + e.getMessage(), e);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Extracción y limpieza (fillna) de un artículo individual
    // -------------------------------------------------------------------------

    private PubmedArticleData extractArticle(Element article) {
        Element citation = firstChild(article, "MedlineCitation");
        Element articleEl = citation != null ? firstChild(citation, "Article") : null;
        Element journal = articleEl != null ? firstChild(articleEl, "Journal") : null;
        Element pubDate = journal != null
                ? firstChild(firstChild(journal, "JournalIssue"), "PubDate")
                : null;

        String pmid = textOf(citation, "PMID", null);
        String titulo = fillna(textOf(articleEl, "ArticleTitle", null));
        String autores = buildAutores(articleEl);
        String revista = fillna(textOf(journal, "Title", null));
        Integer anioPub = parseYear(pubDate);
        String mesPub = fillna(textOf(pubDate, "Month", null));
        String doi = extractDoi(article);
        String abstractText = fillna(textOf(articleEl, "AbstractText", null));
        String meshTerms = extractMeshTerms(citation);
        String keywords = fillna(textOf(citation, "Keyword", null));
        String tipoPublicacion = fillna(extractTipoPublicacion(articleEl));
        String url = buildPubmedUrl(pmid);

        return new PubmedArticleData(
                pmid, titulo, autores, revista,
                anioPub, mesPub, doi,
                abstractText, meshTerms, keywords,
                tipoPublicacion, url);
    }

    private String buildAutores(Element articleEl) {
        if (articleEl == null) return NO_DISPONIBLE;
        NodeList authorList = articleEl.getElementsByTagName("Author");
        if (authorList.getLength() == 0) return NO_DISPONIBLE;
        List<String> names = new ArrayList<>();
        for (int i = 0; i < authorList.getLength(); i++) {
            Element author = (Element) authorList.item(i);
            String lastName = textOf(author, "LastName", "");
            String foreName = textOf(author, "ForeName", "");
            String name = (lastName + " " + foreName).trim();
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names.isEmpty() ? NO_DISPONIBLE : String.join("; ", names);
    }

    private String extractDoi(Element article) {
        NodeList elIds = article.getElementsByTagName("ArticleId");
        for (int i = 0; i < elIds.getLength(); i++) {
            Element el = (Element) elIds.item(i);
            if ("doi".equalsIgnoreCase(el.getAttribute("IdType"))) {
                String doi = el.getTextContent().trim();
                return doi.isBlank() ? null : doi;
            }
        }
        return null; // DOI puede quedar null (no aplicamos fillna aquí para que la lógica de unicidad funcione)
    }

    private String extractMeshTerms(Element citation) {
        if (citation == null) return NO_DISPONIBLE;
        NodeList descriptors = citation.getElementsByTagName("DescriptorName");
        if (descriptors.getLength() == 0) return NO_DISPONIBLE;
        List<String> terms = new ArrayList<>();
        for (int i = 0; i < descriptors.getLength(); i++) {
            String t = descriptors.item(i).getTextContent().trim();
            if (!t.isBlank()) terms.add(t);
        }
        return terms.isEmpty() ? NO_DISPONIBLE : String.join(", ", terms);
    }

    private String extractTipoPublicacion(Element articleEl) {
        if (articleEl == null) return null;
        NodeList types = articleEl.getElementsByTagName("PublicationType");
        if (types.getLength() == 0) return null;
        return types.item(0).getTextContent().trim();
    }

    private String buildPubmedUrl(String pmid) {
        if (pmid == null || pmid.isBlank()) return NO_DISPONIBLE;
        return "https://pubmed.ncbi.nlm.nih.gov/" + pmid + "/";
    }

    // -------------------------------------------------------------------------
    // Utilidades XML
    // -------------------------------------------------------------------------

    private Element firstChild(Element parent, String tag) {
        if (parent == null) return null;
        NodeList nl = parent.getElementsByTagName(tag);
        return nl.getLength() > 0 ? (Element) nl.item(0) : null;
    }

    private String textOf(Element parent, String tag, String defaultValue) {
        if (parent == null) return defaultValue;
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return defaultValue;
        String text = nl.item(0).getTextContent().trim();
        return text.isBlank() ? defaultValue : text;
    }

    private Integer parseYear(Element pubDate) {
        if (pubDate == null) return null;
        String year = textOf(pubDate, "Year", null);
        if (year == null) {
            // Intenta formato "2024 Jan" en MedlineDate
            String medline = textOf(pubDate, "MedlineDate", null);
            if (medline != null && medline.length() >= 4) {
                year = medline.substring(0, 4);
            }
        }
        if (year == null) return null;
        try {
            return Integer.parseInt(year);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Limpieza tipo fillna(): si el valor es nulo o blank, retorna "No disponible".
     */
    private String fillna(String value) {
        return (value == null || value.isBlank()) ? NO_DISPONIBLE : value;
    }
}
