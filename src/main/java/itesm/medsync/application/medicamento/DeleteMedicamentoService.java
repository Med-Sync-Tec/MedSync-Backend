package itesm.medsync.application.medicamento;

import itesm.medsync.domain.medicamento.exception.MedicamentoNotFoundException;
import itesm.medsync.domain.medicamento.repository.MedicamentoRepository;
import itesm.medsync.domain.medicamento.usecase.DeleteMedicamentoUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class DeleteMedicamentoService implements DeleteMedicamentoUseCase {

    private final MedicamentoRepository medicamentoRepository;

    @Inject
    public DeleteMedicamentoService(MedicamentoRepository medicamentoRepository) {
        this.medicamentoRepository = medicamentoRepository;
    }

    @Override
    @Transactional
    public void execute(UUID id) {
        medicamentoRepository.findByUuid(id)
                .orElseThrow(() -> new MedicamentoNotFoundException(id));
        medicamentoRepository.delete(id);
    }
}
