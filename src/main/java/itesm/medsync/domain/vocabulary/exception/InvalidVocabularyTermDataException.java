package itesm.medsync.domain.vocabulary.exception;

/**
 * Thrown when a vocabulary term violates a domain invariant (null tipo, blank
 * or oversized valor). Maps to HTTP 400 for future-proofing, but in this
 * snapshot it only fires at boot — a fired exception aborts the application.
 */
public class InvalidVocabularyTermDataException extends RuntimeException {

    public InvalidVocabularyTermDataException(String message) {
        super(message);
    }
}
