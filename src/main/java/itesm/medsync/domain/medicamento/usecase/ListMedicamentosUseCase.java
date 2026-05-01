package itesm.medsync.domain.medicamento.usecase;

import itesm.medsync.domain.medicamento.model.MedicamentosPage;

public interface ListMedicamentosUseCase {
    MedicamentosPage execute(String nombre, String estado, int page, int size);
}
