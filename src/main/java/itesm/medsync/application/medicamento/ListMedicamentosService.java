package itesm.medsync.application.medicamento;

import itesm.medsync.domain.medicamento.model.MedicamentosPage;
import itesm.medsync.domain.medicamento.repository.MedicamentoRepository;
import itesm.medsync.domain.medicamento.usecase.ListMedicamentosUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ListMedicamentosService implements ListMedicamentosUseCase {

    private final MedicamentoRepository medicamentoRepository;

    @Inject
    public ListMedicamentosService(MedicamentoRepository medicamentoRepository) {
        this.medicamentoRepository = medicamentoRepository;
    }

    @Override
    public MedicamentosPage execute(String nombre, String estado, int page, int size) {
        return medicamentoRepository.findPaginated(nombre, estado, page, size);
    }
}
