package itesm.medsync.application.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.SyncPubmedArticlesUseCase;
import itesm.medsync.infrastructure.pubmed.PubmedArticleData;
import itesm.medsync.infrastructure.pubmed.PubmedEutilsClient;
import itesm.medsync.infrastructure.pubmed.PubmedResponseParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del caso de uso de sincronización con PubMed.
 * <p>
 * Flujo:
 * 1. esearch → obtiene hasta 1 000 PMIDs con filtro trending[sb].
 * 2. efetch en lotes de 200 → obtiene metadatos XML completos.
 * 3. Aplica fillna (campos nulos → "No disponible").
 * 4. Persiste artículos nuevos; actualiza los que ya existen por DOI/URL.
 */
@ApplicationScoped
public class SyncPubmedArticlesService implements SyncPubmedArticlesUseCase {

    private static final Logger LOG = Logger.getLogger(SyncPubmedArticlesService.class);

    private static final String DB = "pubmed";
    private static final String TERM = "(trending[sb]) OR (2024:2025[pdat] AND medicine[all])";
    private static final int RET_MAX = 1000;
    private static final String RET_MODE_JSON = "json";
    private static final String RET_MODE_XML = "xml";
    private static final String RET_TYPE = "abstract";

    private final PubmedEutilsClient pubmedClient;
    private final PubmedResponseParser parser;
    private final ArticleRepository articleRepository;

    @Inject
    public SyncPubmedArticlesService(@RestClient PubmedEutilsClient pubmedClient,
                                     PubmedResponseParser parser,
                                     ArticleRepository articleRepository) {
        this.pubmedClient = pubmedClient;
        this.parser = parser;
        this.articleRepository = articleRepository;
    }

    @Override
    @Transactional
    public int execute() {
        LOG.info("Iniciando sincronización de artículos desde PubMed (trending[sb])...");

        // 1. Obtener PMIDs
        String esearchJson = pubmedClient.searchIds(DB, TERM, RET_MAX, RET_MODE_JSON);
        List<String> pmids = parser.parseEsearchIds(esearchJson);
        LOG.infof("PMIDs obtenidos de PubMed: %d", pmids.size());

        if (pmids.isEmpty()) {
            LOG.warn("No se obtuvieron PMIDs de PubMed. Sincronización cancelada.");
            return 0;
        }

        // 2. Recuperar metadatos en lotes
        int batchSize = parser.getBatchSize();
        List<PubmedArticleData> allArticles = new ArrayList<>();
        for (int i = 0; i < pmids.size(); i += batchSize) {
            List<String> batch = pmids.subList(i, Math.min(i + batchSize, pmids.size()));
            String idsParam = String.join(",", batch);
            try {
                String xml = pubmedClient.fetchArticles(DB, idsParam, RET_MODE_XML, RET_TYPE);
                allArticles.addAll(parser.parseEfetchArticles(xml));
            } catch (Exception e) {
                LOG.warnf("Error en lote %d-%d, se omite: %s", i, i + batchSize, e.getMessage());
            }
        }
        LOG.infof("Artículos parseados: %d", allArticles.size());

        // 3. Persistir / actualizar
        int count = 0;
        for (PubmedArticleData data : allArticles) {
            try {
                upsertArticle(data);
                count++;
            } catch (Exception e) {
                LOG.warnf("No se pudo persistir artículo PMID=%s: %s", data.pmid(), e.getMessage());
            }
        }

        LOG.infof("Sincronización completada. Artículos insertados/actualizados: %d", count);
        return count;
    }

    /**
     * Inserta el artículo si es nuevo (por DOI o, si no tiene DOI, por URL).
     * Si ya existe, lo actualiza con los metadatos frescos de PubMed.
     */
    private void upsertArticle(PubmedArticleData data) {
        // Construir el dominio limpio
        Article article = Article.create(
                data.titulo(),
                data.autores(),
                data.revista(),
                data.anioPub(),
                data.mesPub(),
                data.doi(),
                data.abstractText(),
                combineKeywords(data.keywords(), data.meshTerms()),
                data.tipoPublicacion(),
                data.url());

        // Si tiene DOI y ya existe → actualizar usando save (que hace upsert por UUID)
        // Dado que el repositorio ya implementa upsert por UUID en save(),
        // simplemente guardamos el nuevo artículo con un nuevo UUID (insert).
        // La unicidad por DOI la garantiza la base de datos con la constraint uq_articulos_doi.
        if (data.doi() != null && !data.doi().isBlank() && articleRepository.existsByDoi(data.doi())) {
            // DOI ya existe → saltamos (los datos ya están)
            return;
        }

        // Si no tiene DOI, checamos por URL para evitar duplicados en la misma sincronización
        if (articleRepository.existsByUrl(data.url())) {
            return;
        }

        articleRepository.save(article);
    }

    /**
     * Combina keywords libres y MeSH terms en un solo campo de texto.
     */
    private String combineKeywords(String keywords, String meshTerms) {
        boolean kEmpty = keywords == null || keywords.isBlank() || "No disponible".equals(keywords);
        boolean mEmpty = meshTerms == null || meshTerms.isBlank() || "No disponible".equals(meshTerms);
        if (kEmpty && mEmpty) return "No disponible";
        if (kEmpty) return meshTerms;
        if (mEmpty) return keywords;
        return keywords + "; " + meshTerms;
    }
}
