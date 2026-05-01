package itesm.medsync.domain.pacientecontexto.usecase;

import java.util.UUID;

public interface DeletePacienteContextoUseCase {

    void execute(UUID patientId, UUID contextoId);
}
