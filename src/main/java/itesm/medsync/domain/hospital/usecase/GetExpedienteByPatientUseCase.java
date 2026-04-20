package itesm.medsync.domain.hospital.usecase;

import itesm.medsync.domain.hospital.model.ExpedienteClinico;

import java.util.UUID;

public interface GetExpedienteByPatientUseCase {

    ExpedienteClinico execute(UUID patientId);
}
