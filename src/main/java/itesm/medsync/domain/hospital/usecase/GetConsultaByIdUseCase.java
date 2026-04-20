package itesm.medsync.domain.hospital.usecase;

import itesm.medsync.domain.hospital.model.Consulta;

public interface GetConsultaByIdUseCase {

    Consulta execute(String consultaId);
}
