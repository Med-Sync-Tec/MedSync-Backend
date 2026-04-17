package itesm.medsync.domain.patient.usecase;

import itesm.medsync.domain.patient.model.Patient;

import java.util.UUID;

public interface GetPatientByIdUseCase {

    Patient execute(UUID id);
}
