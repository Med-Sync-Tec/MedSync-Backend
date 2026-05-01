package itesm.medsync.domain.medicamento.usecase;

import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;

import java.util.UUID;

public interface GetMedicamentoByIdUseCase {
    MedicamentoWithEstado execute(UUID id);
}
