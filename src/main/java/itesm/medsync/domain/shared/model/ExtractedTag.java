package itesm.medsync.domain.shared.model;

/**
 * A clinical tag extracted by the LLM after the vocabulary filter has accepted
 * it. The {@code valor} matches the canonical form from the specialty's
 * {@code Vocabulary} (capitalization preserved by the gateway).
 */
public record ExtractedTag(TipoClinico tipo, String valor) {
}
