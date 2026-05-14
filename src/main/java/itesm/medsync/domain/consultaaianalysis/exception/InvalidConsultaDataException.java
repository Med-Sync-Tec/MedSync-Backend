package itesm.medsync.domain.consultaaianalysis.exception;

/**
 * Thrown when a consulta exists but has no analyzable text — every SOAP and
 * adjunct field is null/blank after trim. A consulta with no content is not
 * meaningful to analyze. Maps to HTTP 400.
 */
public class InvalidConsultaDataException extends RuntimeException {

    public InvalidConsultaDataException(String message) {
        super(message);
    }
}
