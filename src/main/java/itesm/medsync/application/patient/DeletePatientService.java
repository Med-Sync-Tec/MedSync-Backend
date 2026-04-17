package itesm.medsync.application.patient;

import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import itesm.medsync.domain.patient.usecase.DeletePatientUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class DeletePatientService implements DeletePatientUseCase {

    private final PatientRepository repository;

    @Inject
    public DeletePatientService(PatientRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID id) {
        Patient patient = repository.findByUuid(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
        repository.save(patient.softDelete());
    }
}
