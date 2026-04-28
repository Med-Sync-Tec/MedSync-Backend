package itesm.medsync.domain.pacientecontexto.usecase;

import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;

import java.util.UUID;

public interface AddPacienteContextoUseCase {

    PacienteContexto execute(UUID pacienteId,
                             PacienteContexto.Tipo tipo,
                             String valor);
}
