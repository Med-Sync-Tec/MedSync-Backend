package itesm.medsync.application.pubmedautospecialty;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.model.AutoAnalysisSummary;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.AutoAnalyzeNewArticlesUseCase;
import itesm.medsync.domain.shared.model.ArticleAnalysisRequest;
import itesm.medsync.domain.shared.model.ArticleAnalysisResult;
import itesm.medsync.domain.shared.model.ExtractedTag;
import itesm.medsync.domain.shared.model.SpecialtyDescriptor;
import itesm.medsync.domain.shared.repository.AiAnalysisGateway;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.repository.VocabularyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Classifies every article that has no specialty assigned yet by invoking the
 * existing AI gateway, then persisting the result.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>NOT {@code @Transactional} at class level — each article's save runs in
 *       its own {@code REQUIRES_NEW} transaction via {@link ArticleSpecialtySaver},
 *       so a gateway failure on article N does not roll back the save of article N-1.</li>
 *   <li>Active specialties and their vocabularies are loaded <em>once</em> before
 *       the loop to avoid N × M redundant DB reads.</li>
 *   <li>The Groq HTTP call happens outside any DB transaction — no connection is
 *       held during the network round-trip.</li>
 *   <li>A single catch-all in the loop ensures one bad article never aborts the rest.</li>
 * </ul>
 */
@ApplicationScoped
public class AutoAnalyzeNewArticlesService implements AutoAnalyzeNewArticlesUseCase {

    private static final Logger LOG = Logger.getLogger(AutoAnalyzeNewArticlesService.class);

    private final ArticleRepository articleRepository;
    private final SpecialtyRepository specialtyRepository;
    private final VocabularyRepository vocabularyRepository;
    private final AiAnalysisGateway aiGateway;
    private final ArticleSpecialtySaver saver;
    private final boolean enabled;
    /** Maximum number of articles to classify per sync pass. */
    private final int maxArticles;
    /** Milliseconds to sleep between consecutive Groq calls to stay within the TPM rate limit. */
    private final long delayBetweenCallsMs;

    @Inject
    public AutoAnalyzeNewArticlesService(
            ArticleRepository articleRepository,
            SpecialtyRepository specialtyRepository,
            VocabularyRepository vocabularyRepository,
            AiAnalysisGateway aiGateway,
            ArticleSpecialtySaver saver,
            @ConfigProperty(name = "medsync.pubmed.auto-specialty.enabled", defaultValue = "true")
            boolean enabled,
            @ConfigProperty(name = "medsync.pubmed.auto-specialty.max-articles", defaultValue = "10")
            int maxArticles,
            @ConfigProperty(name = "medsync.pubmed.auto-specialty.delay-between-calls-ms", defaultValue = "2000")
            long delayBetweenCallsMs) {
        this.articleRepository = articleRepository;
        this.specialtyRepository = specialtyRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.aiGateway = aiGateway;
        this.saver = saver;
        this.enabled = enabled;
        this.maxArticles = maxArticles;
        this.delayBetweenCallsMs = delayBetweenCallsMs;
    }

    @Override
    public AutoAnalysisSummary execute() {
        if (!enabled) {
            LOG.info("Auto-specialty: deshabilitado por configuración (medsync.pubmed.auto-specialty.enabled=false) — omitiendo.");
            return new AutoAnalysisSummary(0, 0, 0, 0);
        }

        List<Article> all = articleRepository.findAllWithoutSpecialty();
        List<Article> candidates = all.size() > maxArticles ? all.subList(0, maxArticles) : all;
        LOG.infof("Auto-specialty: %d artículos candidatos (especialidad_id = NULL), procesando máximo %d.",
                all.size(), candidates.size());

        if (candidates.isEmpty()) {
            return new AutoAnalysisSummary(0, 0, 0, 0);
        }

        // Load specialties + vocabularies once for the whole batch
        List<Specialty> activeSpecialties = specialtyRepository.findAllActive();
        List<SpecialtyDescriptor> descriptors = buildDescriptors(activeSpecialties);
        Map<UUID, Vocabulary> vocabsById = buildVocabMap(activeSpecialties);

        int succeeded = 0;
        int skippedBlank = 0;
        int failed = 0;
        boolean interrupted = false;

        for (Article article : candidates) {
            if (isBlank(article.getTitulo())
                    && isBlank(article.getAbstractText())
                    && isBlank(article.getKeywords())) {
                LOG.warnf("Auto-specialty: artículo %s no tiene texto analizable — omitido.", article.getId());
                skippedBlank++;
                continue;  // No Groq call was made — skip the delay too.
            }

            try {
                ArticleAnalysisRequest request = new ArticleAnalysisRequest(
                        article.getTitulo(),
                        article.getAbstractText(),
                        article.getKeywords(),
                        List.copyOf(descriptors),
                        Map.copyOf(vocabsById));

                ArticleAnalysisResult result = aiGateway.analyzeArticle(request);

                List<ArticleTag> domainTags = new ArrayList<>(result.tags().size());
                for (ExtractedTag t : result.tags()) {
                    domainTags.add(ArticleTag.create(t.tipo(), t.valor()));
                }

                saver.save(article, result.especialidadId(), domainTags);

                LOG.infof("Auto-specialty: artículo %s → especialidad %s (hadAbstract=%b).",
                        article.getId(), result.especialidadId(), result.hadAbstract());
                succeeded++;

            } catch (Exception e) {
                LOG.errorf(e, "Auto-specialty: fallo al analizar artículo %s: %s",
                        article.getId(), e.getMessage());
                failed++;
            }

            // Always pause after a Groq call (success OR failure) to stay within
            // the tokens-per-minute rate limit. Skipped-blank articles never reach here.
            if (delayBetweenCallsMs > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(delayBetweenCallsMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOG.warn("Auto-specialty: hilo interrumpido durante el delay entre llamadas.");
                    interrupted = true;
                    break;
                }
            }
        }

        if (interrupted) {
            LOG.warn("Auto-specialty: pase interrumpido prematuramente.");
        }


        AutoAnalysisSummary summary = new AutoAnalysisSummary(
                candidates.size(), succeeded, skippedBlank, failed);
        LOG.infof("Auto-specialty: pase completado. %s", summary);
        return summary;
    }

    // ------------------------------------------------------------------ helpers

    private List<SpecialtyDescriptor> buildDescriptors(List<Specialty> specialties) {
        List<SpecialtyDescriptor> descriptors = new ArrayList<>(specialties.size());
        for (Specialty s : specialties) {
            descriptors.add(new SpecialtyDescriptor(s.getId(), s.getNombre(), s.getSlug(), s.getDescripcion()));
        }
        return descriptors;
    }

    private Map<UUID, Vocabulary> buildVocabMap(List<Specialty> specialties) {
        Map<UUID, Vocabulary> map = new HashMap<>();
        for (Specialty s : specialties) {
            map.put(s.getId(), vocabularyRepository.getVocabularyFor(s.getId()));
        }
        return map;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
