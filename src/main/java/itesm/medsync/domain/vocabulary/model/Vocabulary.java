package itesm.medsync.domain.vocabulary.model;

import itesm.medsync.domain.shared.model.TipoClinico;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory, per-specialty controlled vocabulary used to constrain AI tag
 * extraction in features 3 and 4.
 *
 * The aggregate is immutable. The constructor normalizes the input map so
 * every {@link TipoClinico} key is present (missing buckets become
 * {@code List.of()}), and defensively copies every list so callers cannot
 * mutate the stored state by holding onto the original input reference.
 *
 * Lookups are case-insensitive and whitespace-tolerant. Stored values
 * preserve their original capitalization, since the LLM prompt and the debug
 * endpoint emit the canonical form verbatim.
 */
public final class Vocabulary {

    public static final String EMPTY_VERSION = "empty";

    private final UUID especialidadId;
    private final String especialidadSlug;
    private final String version;
    private final Map<TipoClinico, List<VocabularyTerm>> termsByType;

    public Vocabulary(UUID especialidadId,
                      String especialidadSlug,
                      String version,
                      Map<TipoClinico, List<VocabularyTerm>> termsByType) {
        Objects.requireNonNull(especialidadId, "especialidadId");
        Objects.requireNonNull(especialidadSlug, "especialidadSlug");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(termsByType, "termsByType");

        // Defensive normalization: copy every bucket so external mutation of the
        // input does not leak into the aggregate, and pad missing keys with
        // List.of() so callers never need a null check on getTermsFor(...).
        Map<TipoClinico, List<VocabularyTerm>> normalized = new EnumMap<>(TipoClinico.class);
        for (TipoClinico tipo : TipoClinico.values()) {
            List<VocabularyTerm> bucket = termsByType.get(tipo);
            normalized.put(tipo, bucket == null ? List.of() : List.copyOf(bucket));
        }

        this.especialidadId = especialidadId;
        this.especialidadSlug = especialidadSlug;
        this.version = version;
        this.termsByType = Map.copyOf(normalized);
    }

    /**
     * Returns a vocabulary with no terms in any bucket and {@code version = "empty"}.
     * Used as the fallback when a specialty has no JSON file loaded — callers can
     * still call {@code totalTerms()}, {@code containsTerm(...)}, etc. without a null check.
     */
    public static Vocabulary empty(UUID especialidadId, String especialidadSlug) {
        return new Vocabulary(especialidadId, especialidadSlug, EMPTY_VERSION, Map.of());
    }

    public UUID getEspecialidadId() {
        return especialidadId;
    }

    public String getEspecialidadSlug() {
        return especialidadSlug;
    }

    public String getVersion() {
        return version;
    }

    /** Returns the (possibly empty) immutable list of terms for the given clinical type. */
    public List<VocabularyTerm> getTermsFor(TipoClinico tipo) {
        return termsByType.getOrDefault(tipo, List.of());
    }

    /** Sum of all bucket sizes. {@code 0} for an {@link #empty(UUID, String)} vocabulary. */
    public int totalTerms() {
        int total = 0;
        for (List<VocabularyTerm> bucket : termsByType.values()) {
            total += bucket.size();
        }
        return total;
    }

    /**
     * Case-insensitive, trim-tolerant membership check.
     *
     * Used by AI flows to validate that an LLM-extracted tag belongs to the
     * constrained set — the LLM may capitalize inconsistently or add stray
     * whitespace, which we want to tolerate without weakening the vocabulary itself.
     */
    public boolean containsTerm(TipoClinico tipo, String valor) {
        if (tipo == null || valor == null || valor.isBlank()) {
            return false;
        }
        String normalized = valor.trim().toLowerCase();
        for (VocabularyTerm term : getTermsFor(tipo)) {
            if (term.getValor().trim().toLowerCase().equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Flat set of canonical {@code valor} strings for the given type, preserving
     * original capitalization. Suitable for direct inclusion in an LLM prompt.
     */
    public Set<String> allValoresFor(TipoClinico tipo) {
        Set<String> set = new LinkedHashSet<>();
        for (VocabularyTerm term : getTermsFor(tipo)) {
            set.add(term.getValor());
        }
        return Set.copyOf(set);
    }
}
