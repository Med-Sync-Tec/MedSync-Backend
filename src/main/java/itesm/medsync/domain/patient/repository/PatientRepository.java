package itesm.medsync.domain.patient.repository;

import itesm.medsync.domain.patient.model.Patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository {

    Patient save(Patient patient);

    Optional<Patient> findByUuid(UUID id);

    List<Patient> findAllActive();

    boolean existsByExpedienteExternoId(String expedienteExternoId);
}
