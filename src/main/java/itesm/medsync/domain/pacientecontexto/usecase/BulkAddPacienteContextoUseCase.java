package itesm.medsync.domain.pacientecontexto.usecase;

import itesm.medsync.domain.pacientecontexto.model.BulkEntry;
import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.user.model.User;

import java.util.List;
import java.util.UUID;

/**
 * Persists multiple {@link PacienteContexto} rows atomically — all entries
 * succeed and commit together, or none do. Each persisted row inherits the
 * {@code caller}'s {@code especialidadId} (may be {@code null} if the
 * caller has no specialty).
 */
public interface BulkAddPacienteContextoUseCase {

    List<PacienteContexto> execute(UUID patientId, List<BulkEntry> entries, User caller);
}
