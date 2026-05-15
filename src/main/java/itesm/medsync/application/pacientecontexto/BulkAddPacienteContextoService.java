package itesm.medsync.application.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.exception.InvalidPacienteContextoDataException;
import itesm.medsync.domain.pacientecontexto.model.BulkEntry;
import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.pacientecontexto.repository.PacienteContextoRepository;
import itesm.medsync.domain.pacientecontexto.usecase.BulkAddPacienteContextoUseCase;
import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.repository.PatientRepository;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.user.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persists a batch of {@link PacienteContexto} rows atomically. The
 * {@code @Transactional} boundary guarantees that a validation failure on
 * entry #N rolls back the rows inserted for #1..N-1 — the frontend's bulk
 * call either fully succeeds or has no DB side effect.
 *
 * The caller's specialty is the single source of truth for the persisted
 * {@code especialidadId}; the original consulta-writer's specialty is
 * intentionally not consulted (see {@code design.md} for rationale).
 */
@ApplicationScoped
@Transactional
public class BulkAddPacienteContextoService implements BulkAddPacienteContextoUseCase {

    private final PatientRepository patientRepository;
    private final PacienteContextoRepository pcRepository;

    @Inject
    public BulkAddPacienteContextoService(PatientRepository patientRepository,
                                          PacienteContextoRepository pcRepository) {
        this.patientRepository = patientRepository;
        this.pcRepository = pcRepository;
    }

    @Override
    public List<PacienteContexto> execute(UUID patientId, List<BulkEntry> entries, User caller) {
        patientRepository.findByUuid(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        if (entries == null || entries.isEmpty()) {
            throw new InvalidPacienteContextoDataException(
                    "entries cannot be null or empty");
        }

        UUID especialidadId = caller.getEspecialidadId();
        List<PacienteContexto> persisted = new ArrayList<>(entries.size());
        for (BulkEntry entry : entries) {
            // TipoClinico.fromString throws InvalidTipoClinicoException on a
            // bad tipo; PacienteContexto.create throws
            // InvalidPacienteContextoDataException on a blank or oversized
            // valor. Either fires inside the @Transactional, rolling back
            // any rows already inserted in this batch.
            TipoClinico tipo = TipoClinico.fromString(entry.tipo());
            PacienteContexto pc = PacienteContexto.create(patientId, tipo, entry.valor(), especialidadId);
            persisted.add(pcRepository.save(pc));
        }
        return persisted;
    }
}
