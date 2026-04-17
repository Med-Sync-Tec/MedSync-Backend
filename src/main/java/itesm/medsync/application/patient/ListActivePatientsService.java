package itesm.medsync.application.patient;

import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import itesm.medsync.domain.patient.usecase.ListActivePatientsUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ListActivePatientsService implements ListActivePatientsUseCase {

    private final PatientRepository repository;

    @Inject
    public ListActivePatientsService(PatientRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Patient> execute() {
        return repository.findAllActive();
    }
}
