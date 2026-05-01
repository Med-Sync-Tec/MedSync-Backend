package itesm.medsync.infrastructure.pubmed;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Cliente MicroProfile REST para la API E-utilities de NCBI (PubMed).
 * Base URL configurada en application.properties como:
 *   quarkus.rest-client."itesm.medsync.infrastructure.pubmed.PubmedEutilsClient".url
 */
@RegisterRestClient(configKey = "pubmed-eutils")
public interface PubmedEutilsClient {

    /**
     * esearch.fcgi — Busca artículos y devuelve PMIDs en formato JSON.
     *
     * Ejemplo de llamada equivalente:
     *   GET https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi
     *       ?db=pubmed&term=trending[sb]&retmax=1000&retmode=json
     */
    @GET
    @Path("/esearch.fcgi")
    String searchIds(@QueryParam("db") String db,
                     @QueryParam("term") String term,
                     @QueryParam("retmax") int retmax,
                     @QueryParam("retmode") String retmode);

    /**
     * efetch.fcgi — Recupera los datos completos de una lista de PMIDs en XML.
     *
     * Ejemplo de llamada equivalente:
     *   GET https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi
     *       ?db=pubmed&id=12345,67890&retmode=xml&rettype=abstract
     */
    @GET
    @Path("/efetch.fcgi")
    String fetchArticles(@QueryParam("db") String db,
                         @QueryParam("id") String ids,
                         @QueryParam("retmode") String retmode,
                         @QueryParam("rettype") String rettype);
}
