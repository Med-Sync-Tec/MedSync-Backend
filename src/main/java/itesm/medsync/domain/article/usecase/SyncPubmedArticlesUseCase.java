package itesm.medsync.domain.article.usecase;

/**
 * Caso de uso: disparar la sincronización de artículos trending desde PubMed
 * y persistirlos/actualizarlos en la base de datos local.
 */
public interface SyncPubmedArticlesUseCase {

    /**
     * Ejecuta la sincronización completa:
     * 1. Consulta esearch.fcgi con filtro trending[sb].
     * 2. Llama a efetch.fcgi para obtener los metadatos completos.
     * 3. Aplica limpieza (fillna) y persiste artículos nuevos o actualiza los existentes.
     *
     * @return número de artículos nuevos insertados/actualizados
     */
    int execute();
}
