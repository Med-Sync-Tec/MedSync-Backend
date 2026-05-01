package itesm.medsync.domain.pacientecontexto.usecase;

import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.shared.model.TipoClinico;

import java.util.UUID;

public interface AddPacienteContextoUseCase {

    PacienteContexto execute(UUID pacienteId, TipoClinico tipo, String valor);
}
