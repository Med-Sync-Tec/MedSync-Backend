package itesm.medsync.domain.hospital.usecase;

import itesm.medsync.domain.hospital.model.Consulta;

import java.util.List;
import java.util.UUID;

public interface GetConsultasByPatientUseCase {

    List<Consulta> execute(UUID patientId);
}
