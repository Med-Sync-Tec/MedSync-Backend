package itesm.medsync.domain.hospital.usecase;

import itesm.medsync.domain.hospital.model.Consulta;

import java.util.UUID;

public interface CreateConsultaUseCase {

    Consulta execute(UUID patientId, CreateConsultaCommand cmd);
}
