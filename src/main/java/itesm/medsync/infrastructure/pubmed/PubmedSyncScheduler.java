package itesm.medsync.infrastructure.pubmed;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import itesm.medsync.domain.article.model.AutoAnalysisSummary;
import itesm.medsync.domain.article.usecase.AutoAnalyzeNewArticlesUseCase;
import itesm.medsync.domain.article.usecase.SyncPubmedArticlesUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Job programado que dispara la sincronización de artículos de PubMed una vez al día.
 * <p>
 * La expresión cron {@code "0 0 12 * * ?"} se ejecuta todos los días a las 12:00.
 * <p>
 * En prod tanto el cron ({@code %prod.quarkus.scheduler.enabled=false}) como el
 * startup sync ({@code %prod.medsync.pubmed.sync.on-startup=false}) quedan
 * deshabilitados. El seed inicial se realiza manualmente via POST /api/articles/sync.
 * <p>
 * En perfil de test el scheduler queda deshabilitado via
 * {@code %test.quarkus.scheduler.enabled=false} para no disparar llamadas reales.
 */
@ApplicationScoped
public class PubmedSyncScheduler {

    private static final Logger LOG = Logger.getLogger(PubmedSyncScheduler.class);

    private final SyncPubmedArticlesUseCase syncUseCase;
    private final AutoAnalyzeNewArticlesUseCase autoAnalyzeUseCase;

    @ConfigProperty(name = "medsync.pubmed.sync.on-startup", defaultValue = "true")
    boolean syncOnStartup;

    @Inject
    public PubmedSyncScheduler(SyncPubmedArticlesUseCase syncUseCase,
                               AutoAnalyzeNewArticlesUseCase autoAnalyzeUseCase) {
        this.syncUseCase = syncUseCase;
        this.autoAnalyzeUseCase = autoAnalyzeUseCase;
    }

    /**
     * Dispara una sincronización inicial al arrancar la aplicación, si está habilitada.
     */
    void onStart(@Observes StartupEvent ev) {
        if (!syncOnStartup) {
            LOG.info("Startup: sincronización PubMed deshabilitada por configuración (medsync.pubmed.sync.on-startup=false).");
            return;
        }
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
            // Do not proceed to auto-specialty if sync itself failed
            return;
        }

        LOG.info("Scheduler: iniciando pase de auto-especialidad...");
        try {
            AutoAnalysisSummary summary = autoAnalyzeUseCase.execute();
            LOG.infof("Scheduler: auto-especialidad completado. %s", summary);
        } catch (Exception e) {
            // A failure in the auto-specialty pass must not affect the sync outcome.
            LOG.errorf(e, "Scheduler: error en pase de auto-especialidad: %s", e.getMessage());
        }
    }
}
