package itesm.medsync.domain.medicamento.usecase;

import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;

import java.util.UUID;

public interface UpdateMedicamentoUseCase {
    MedicamentoWithEstado execute(UUID id, String nombre, String estadoNombre, String descripcion);
}
