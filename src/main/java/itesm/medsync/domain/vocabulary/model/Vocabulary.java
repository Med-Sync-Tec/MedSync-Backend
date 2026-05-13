package itesm.medsync.domain.vocabulary.model;

import itesm.medsync.domain.shared.model.TipoClinico;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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

    public List<VocabularyTerm> getTermsFor(TipoClinico tipo) {
        return termsByType.getOrDefault(tipo, List.of());
    }

    public int totalTerms() {
        int total = 0;
        for (List<VocabularyTerm> bucket : termsByType.values()) {
            total += bucket.size();
        }
        return total;
    }

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

    public Set<String> allValoresFor(TipoClinico tipo) {
        Set<String> set = new LinkedHashSet<>();
        for (VocabularyTerm term : getTermsFor(tipo)) {
            set.add(term.getValor());
        }
        return Set.copyOf(set);
    }
}
