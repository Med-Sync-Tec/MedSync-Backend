package itesm.medsync.infrastructure.vocabulary;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.util.ClassPathUtils;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.model.VocabularyTerm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Boot-time loader: enumerates {@code vocabulary/*.json} on the classpath,
 * parses each file, builds {@link Vocabulary} aggregates, and installs the
 * resulting map into {@link VocabularyRepositoryImpl}.
 *
 * <p><strong>Fail-fast vs. soft-skip policy</strong> (see design.md §3 / §4):
 * <ul>
 *   <li>Schema violations (typo in a TipoClinico key, duplicate term, oversize value)
 *       throw and abort application startup. A clinical-content mistake must never
 *       silently degrade AI extraction in production.</li>
 *   <li>Cross-reference mismatches (JSON for a specialty that no longer exists in the
 *       DB; specialty in the DB with no JSON yet) only log — they are legitimate
 *       operational states during onboarding or after a specialty soft-delete.</li>
 * </ul>
 *
 * <p>The {@link #build(Map, List)} method is package-private to expose a pure
 * seam for unit tests; the {@link #onStart} observer wraps it with classpath I/O
 * and CDI wiring.
 */
@ApplicationScoped
public class VocabularyLoader {

    private static final Logger LOG = Logger.getLogger(VocabularyLoader.class);
    private static final String VOCABULARY_RESOURCE_DIR = "vocabulary";
    private static final String JSON_SUFFIX = ".json";

    private final VocabularyJsonReader reader;
    private final SpecialtyRepository specialtyRepository;
    private final VocabularyRepositoryImpl repository;

    @Inject
    public VocabularyLoader(VocabularyJsonReader reader,
                            SpecialtyRepository specialtyRepository,
                            VocabularyRepositoryImpl repository) {
        this.reader = reader;
        this.specialtyRepository = specialtyRepository;
        this.repository = repository;
    }

    /** Test-only constructor: lets unit tests drive {@link #build(Map, List)} without CDI. */
    VocabularyLoader(VocabularyJsonReader reader) {
        this(reader, null, null);
    }

    /**
     * Quarkus startup observer. Failure here aborts the application boot, which is
     * the intended behavior for any schema violation in a vocabulary file.
     */
    void onStart(@Observes StartupEvent ev) {
        // Query the DB first: the loader pairs files to specialties by slug, so the
        // DB roster is the source of truth for which slugs are valid. A specialty
        // without a file is allowed; a file without a matching specialty is skipped.
        List<Specialty> specialties = specialtyRepository.findAllActive();
        Map<String, UUID> slugToId = new HashMap<>();
        for (Specialty s : specialties) {
            slugToId.put(s.getSlug(), s.getId());
        }

        List<RawFile> rawFiles = readClasspathFiles();
        BuildResult result = build(slugToId, rawFiles);
        repository.installMap(result.loaded());

        int totalTerms = result.loaded().values().stream().mapToInt(Vocabulary::totalTerms).sum();
        int missingFiles = (int) specialties.stream()
                .filter(s -> !result.loaded().containsKey(s.getId()))
                .count();
        LOG.infof("Loaded vocabulary for %d specialties (%d terms total). %d specialties have no vocabulary file. %d stale files were skipped.",
                result.loaded().size(), totalTerms, missingFiles, result.staleSlugs().size());

        for (Specialty s : specialties) {
            if (!result.loaded().containsKey(s.getId())) {
                LOG.infof("No vocabulary file for specialty '%s' — AI features will see an empty vocabulary.", s.getSlug());
            }
        }
        for (String stale : result.staleSlugs()) {
            LOG.warnf("Vocabulary file '%s.json' has no matching specialty in DB — skipping (possibly a stale file).", stale);
        }
    }

    /**
     * Enumerates classpath resources under {@code vocabulary/}. Uses Quarkus's
     * {@link ClassPathUtils} so it works uniformly in dev mode (filesystem),
     * tests, and packaged JARs (entries inside the jar). Catastrophic failures
     * (classpath unreadable) propagate and rightly abort the boot.
     */
    private List<RawFile> readClasspathFiles() {
        List<RawFile> files = new ArrayList<>();
        try {
            ClassPathUtils.consumeAsPaths(VOCABULARY_RESOURCE_DIR, dir -> collectFromDirectory(dir, files));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to enumerate vocabulary classpath resources", ex);
        }
        return files;
    }

    private void collectFromDirectory(Path dir, List<RawFile> sink) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(JSON_SUFFIX))
                    .forEach(p -> {
                        try {
                            String filename = p.getFileName().toString();
                            String slug = filename.substring(0, filename.length() - JSON_SUFFIX.length());
                            sink.add(new RawFile(slug, Files.readAllBytes(p)));
                        } catch (IOException ex) {
                            throw new IllegalStateException("Failed to read vocabulary file " + p, ex);
                        }
                    });
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to list vocabulary directory " + dir, ex);
        }
    }

    /**
     * Pure-logic seam: takes the already-resolved slug → id map and the
     * already-read file bytes, and returns the result. Exposed at
     * package-private visibility so unit tests can drive every branch
     * (valid file, stale slug, malformed JSON) without booting Quarkus.
     *
     * Parse exceptions bubble up unchanged — the boot observer relies on that
     * to abort startup with a clear message.
     */
    BuildResult build(Map<String, UUID> slugToId, List<RawFile> files) {
        Map<UUID, Vocabulary> loaded = new HashMap<>();
        List<String> stale = new ArrayList<>();

        for (RawFile file : files) {
            ParsedVocabularyFile parsed = reader.parse(file.slug(), file.bytes());
            UUID specialtyId = slugToId.get(parsed.slug());
            if (specialtyId == null) {
                // Soft-skip: file is structurally valid but its specialty no longer
                // exists in the DB. Typically a stale file for a soft-deleted specialty.
                stale.add(parsed.slug());
                continue;
            }
            loaded.put(specialtyId, toVocabulary(parsed, specialtyId));
        }

        return new BuildResult(Map.copyOf(loaded), List.copyOf(stale));
    }

    private Vocabulary toVocabulary(ParsedVocabularyFile parsed, UUID specialtyId) {
        Map<TipoClinico, List<VocabularyTerm>> termsByType = new EnumMap<>(TipoClinico.class);
        for (Map.Entry<TipoClinico, List<String>> entry : parsed.termsByType().entrySet()) {
            List<VocabularyTerm> terms = new ArrayList<>(entry.getValue().size());
            for (String valor : entry.getValue()) {
                terms.add(new VocabularyTerm(entry.getKey(), valor));
            }
            termsByType.put(entry.getKey(), List.copyOf(terms));
        }
        return new Vocabulary(specialtyId, parsed.slug(), parsed.version(), termsByType);
    }

    /** A vocabulary file read from the classpath: its slug (from the filename) and its raw bytes. */
    public record RawFile(String slug, byte[] bytes) {
    }

    /** Outcome of {@link #build(Map, List)}: the loaded map plus any file slugs that had no matching specialty. */
    record BuildResult(Map<UUID, Vocabulary> loaded, List<String> staleSlugs) {
    }
}
