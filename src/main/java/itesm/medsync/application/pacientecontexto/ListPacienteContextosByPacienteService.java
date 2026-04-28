package itesm.medsync.application.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.pacientecontexto.repository.PacienteContextoRepository;
import itesm.medsync.domain.pacientecontexto.usecase.ListPacienteContextosByPacienteUseCase;
import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.repository.PatientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListPacienteContextosByPacienteService implements ListPacienteContextosByPacienteUseCase {

    private final PacienteContextoRepository repository;
    private final PatientRepository patientRepository;

    @Inject
    public ListPacienteContextosByPacienteService(PacienteContextoRepository repository,
                                                  PatientRepository patientRepository) {
        this.repository = repository;
        this.patientRepository = patientRepository;
    }

    @Override
    public List<PacienteContexto> execute(UUID pacienteId) {
        patientRepository.findByUuid(pacienteId)
                .orElseThrow(() -> new PatientNotFoundException(pacienteId));
        return repository.findByPacienteId(pacienteId);
    }
}
