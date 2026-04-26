package itesm.medsync.application.patient;

import itesm.medsync.domain.patient.exception.DuplicatePatientException;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import itesm.medsync.domain.patient.usecase.CreatePatientUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.UUID;

@ApplicationScoped
public class CreatePatientService implements CreatePatientUseCase {

    private final PatientRepository repository;

    @Inject
    public CreatePatientService(PatientRepository repository) {
        this.repository = repository;
    }

    @Override
    public Patient execute(String expedienteExternoId,
                           String nombre,
                           LocalDate fechaNacimiento,
                           String genero,
                           UUID medicoId) {
        if (expedienteExternoId != null && repository.existsByExpedienteExternoId(expedienteExternoId)) {
            throw new DuplicatePatientException(expedienteExternoId);
        }
        Patient patient = Patient.create(expedienteExternoId, nombre, fechaNacimiento, genero, medicoId);
        return repository.save(patient);
    }
}
