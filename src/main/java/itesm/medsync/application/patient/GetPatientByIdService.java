package itesm.medsync.application.patient;

import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import itesm.medsync.domain.patient.usecase.GetPatientByIdUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class GetPatientByIdService implements GetPatientByIdUseCase {

    private final PatientRepository repository;

    @Inject
    public GetPatientByIdService(PatientRepository repository) {
        this.repository = repository;
    }

    @Override
    public Patient execute(UUID id) {
        return repository.findByUuid(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }
}
