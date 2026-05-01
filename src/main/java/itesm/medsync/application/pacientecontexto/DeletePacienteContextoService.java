package itesm.medsync.application.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.exception.PacienteContextoNotFoundException;
import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.pacientecontexto.repository.PacienteContextoRepository;
import itesm.medsync.domain.pacientecontexto.usecase.DeletePacienteContextoUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class DeletePacienteContextoService implements DeletePacienteContextoUseCase {

    private final PacienteContextoRepository repository;

    @Inject
    public DeletePacienteContextoService(PacienteContextoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID patientId, UUID contextoId) {
        PacienteContexto contexto = repository.findByUuid(contextoId)
                .orElseThrow(() -> new PacienteContextoNotFoundException(contextoId));

        if (!contexto.getPacienteId().equals(patientId)) {
            // El contexto existe pero no pertenece al paciente del path; no filtramos esa
            // diferencia al cliente para no permitir enumeración.
            throw new PacienteContextoNotFoundException(contextoId);
        }

        repository.removeById(contextoId);
    }
}
