package itesm.medsync.domain.pacientecontexto.repository;

import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PacienteContextoRepository {

    PacienteContexto save(PacienteContexto contexto);

    Optional<PacienteContexto> findByUuid(UUID id);

    List<PacienteContexto> findByPacienteId(UUID pacienteId);

    void removeById(UUID id);
}
