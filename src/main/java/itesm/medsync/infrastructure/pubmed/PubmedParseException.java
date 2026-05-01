package itesm.medsync.infrastructure.pubmed;

/**
 * Excepción lanzada cuando el parser de respuestas de PubMed encuentra un error.
 */
public class PubmedParseException extends RuntimeException {

    public PubmedParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
