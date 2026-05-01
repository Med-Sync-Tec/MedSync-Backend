package itesm.medsync.infrastructure.pubmed;

/**
 * DTO interno que representa los datos de un artículo tal como vienen de PubMed
 * luego de la limpieza (fillna).
 */
public record PubmedArticleData(
        String pmid,
        String titulo,
        String autores,
        String revista,
        Integer anioPub,
        String mesPub,
        String doi,
        String abstractText,
        String meshTerms,
        String keywords,
        String tipoPublicacion,
        String url
) {
}
