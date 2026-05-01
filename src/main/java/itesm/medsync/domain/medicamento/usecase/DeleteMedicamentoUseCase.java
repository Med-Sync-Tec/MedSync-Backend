package itesm.medsync.domain.medicamento.usecase;

import java.util.UUID;

public interface DeleteMedicamentoUseCase {
    void execute(UUID id);
}
