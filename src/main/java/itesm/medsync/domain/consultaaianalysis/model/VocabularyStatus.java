package itesm.medsync.domain.consultaaianalysis.model;

/**
 * Whether the doctor's specialty had a non-empty controlled vocabulary at
 * analysis time. {@code EMPTY} short-circuits the LLM call entirely — the
 * gateway is never invoked and the response carries zero suggestions.
 */
public enum VocabularyStatus {
    POPULATED,
    EMPTY
}
