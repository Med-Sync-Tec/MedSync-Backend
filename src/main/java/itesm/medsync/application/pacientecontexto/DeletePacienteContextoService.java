package itesm.medsync.application.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.exception.PacienteContextoNotFoundException;
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
    public void execute(UUID id) {
        repository.findByUuid(id)
                .orElseThrow(() -> new PacienteContextoNotFoundException(id));
        repository.removeById(id);
    }
}
