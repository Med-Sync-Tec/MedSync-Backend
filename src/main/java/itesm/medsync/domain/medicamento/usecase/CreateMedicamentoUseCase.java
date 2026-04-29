package itesm.medsync.domain.medicamento.usecase;

import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;

public interface CreateMedicamentoUseCase {
    MedicamentoWithEstado execute(String nombre, String descripcion);
}
