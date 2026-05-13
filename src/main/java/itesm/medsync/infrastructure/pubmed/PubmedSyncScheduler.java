package itesm.medsync.infrastructure.pubmed;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import itesm.medsync.domain.article.usecase.SyncPubmedArticlesUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Job programado que dispara la sincronización de artículos de PubMed una vez al día.
 * <p>
 * La expresión cron {@code "0 0 12 * * ?"} se ejecuta todos los días a las 12:00.
 * <p>
 * En perfil de test el scheduler queda deshabilitado via
 * {@code %test.quarkus.scheduler.enabled=false} para no disparar llamadas reales.
 */
@ApplicationScoped
public class PubmedSyncScheduler {

    private static final Logger LOG = Logger.getLogger(PubmedSyncScheduler.class);

    private final SyncPubmedArticlesUseCase syncUseCase;

    @Inject
    public PubmedSyncScheduler(SyncPubmedArticlesUseCase syncUseCase) {
        this.syncUseCase = syncUseCase;
    }

    /**
     * Dispara una sincronización inicial al arrancar la aplicación.
     */
    void onStart(@Observes StartupEvent ev) {
        LOG.info("Startup: disparando sincronización inicial de PubMed...");
        syncPubmedArticles();
    }

    @Scheduled(cron = "0 0 12 * * ?", identity = "pubmed-sync-job")
    void syncPubmedArticles() {
        LOG.info("Scheduler: iniciando sincronización PubMed...");
        try {
            int count = syncUseCase.execute();
            LOG.infof("Scheduler: sincronización completada. Artículos procesados: %d", count);
        } catch (Exception e) {
            LOG.errorf(e, "Scheduler: error durante la sincronización PubMed: %s", e.getMessage());
        }
    }
}
