package itesm.medsync.application.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.pacientecontexto.repository.PacienteContextoRepository;
import itesm.medsync.domain.pacientecontexto.usecase.AddPacienteContextoUseCase;
import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.repository.PatientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class AddPacienteContextoService implements AddPacienteContextoUseCase {

    private final PacienteContextoRepository repository;
    private final PatientRepository patientRepository;

    @Inject
    public AddPacienteContextoService(PacienteContextoRepository repository,
                                      PatientRepository patientRepository) {
        this.repository = repository;
        this.patientRepository = patientRepository;
    }

    @Override
    public PacienteContexto execute(UUID pacienteId,
                                    PacienteContexto.Tipo tipo,
                                    String valor) {
        patientRepository.findByUuid(pacienteId)
                .orElseThrow(() -> new PatientNotFoundException(pacienteId));
        PacienteContexto contexto = PacienteContexto.create(pacienteId, tipo, valor);
        return repository.save(contexto);
    }
}
