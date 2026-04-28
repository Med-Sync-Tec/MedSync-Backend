package itesm.medsync.domain.pacientecontexto.usecase;

import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;

import java.util.List;
import java.util.UUID;

public interface ListPacienteContextosByPacienteUseCase {

    List<PacienteContexto> execute(UUID pacienteId);
}
