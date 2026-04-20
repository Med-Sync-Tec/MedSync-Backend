package itesm.medsync.domain.hospital.repository;

import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.model.ExpedienteClinico;

import java.util.List;
import java.util.Optional;

public interface HospitalGateway {

    Optional<ExpedienteClinico> findExpedienteByPacienteExternoId(String pacienteExternoId);

    List<Consulta> findConsultasByPacienteExternoId(String pacienteExternoId);

    Optional<Consulta> findConsultaById(String consultaId);

    ExpedienteClinico saveExpediente(ExpedienteClinico expediente);

    Consulta saveConsulta(Consulta consulta);
}
