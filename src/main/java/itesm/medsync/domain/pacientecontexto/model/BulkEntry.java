package itesm.medsync.domain.pacientecontexto.model;

/**
 * One row of a bulk patient-context add request.
 *
 * Stays in the domain so {@link itesm.medsync.domain.pacientecontexto.usecase.BulkAddPacienteContextoUseCase}
 * remains framework-free. {@code tipo} is a raw {@code String} (not yet a
 * {@code TipoClinico}) because parsing is a service-layer concern that
 * surfaces a user-facing {@code InvalidPacienteContextoDataException} on
 * failure.
 */
public record BulkEntry(String tipo, String valor) {
}
