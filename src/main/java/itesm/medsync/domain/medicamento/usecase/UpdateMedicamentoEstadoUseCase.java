package itesm.medsync.domain.medicamento.usecase;

import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;

import java.util.UUID;

public interface UpdateMedicamentoEstadoUseCase {
    MedicamentoWithEstado execute(UUID medicamentoId, String nuevoEstadoNombre);
}
