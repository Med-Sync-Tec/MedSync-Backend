package itesm.medsync.interfaces.rest.consultaaianalysis;

/**
 * Single AI suggestion for a consulta. Shape mirrors the bulk-add entry
 * DTO so the frontend can hand suggestions back to the bulk endpoint
 * verbatim once the doctor accepts them.
 */
public record SuggestionResponse(String tipo, String valor) {
}
