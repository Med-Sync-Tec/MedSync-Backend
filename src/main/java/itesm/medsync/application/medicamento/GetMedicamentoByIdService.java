package itesm.medsync.application.medicamento;

import itesm.medsync.domain.medicamento.exception.MedicamentoNotFoundException;
import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;
import itesm.medsync.domain.medicamento.repository.MedicamentoRepository;
import itesm.medsync.domain.medicamento.usecase.GetMedicamentoByIdUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class GetMedicamentoByIdService implements GetMedicamentoByIdUseCase {

    private final MedicamentoRepository medicamentoRepository;

    @Inject
    public GetMedicamentoByIdService(MedicamentoRepository medicamentoRepository) {
        this.medicamentoRepository = medicamentoRepository;
    }

    @Override
    public MedicamentoWithEstado execute(UUID id) {
        return medicamentoRepository.findByUuid(id)
                .orElseThrow(() -> new MedicamentoNotFoundException(id));
    }
}
