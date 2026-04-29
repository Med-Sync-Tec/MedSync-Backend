package itesm.medsync.application.medicamento;

import itesm.medsync.domain.medicamento.exception.EstadoNotFoundException;
import itesm.medsync.domain.medicamento.exception.MedicamentoNotFoundException;
import itesm.medsync.domain.medicamento.model.Medicamento;
import itesm.medsync.domain.medicamento.model.MedicamentoEstado;
import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;
import itesm.medsync.domain.medicamento.repository.MedicamentoEstadoRepository;
import itesm.medsync.domain.medicamento.repository.MedicamentoRepository;
import itesm.medsync.domain.medicamento.usecase.UpdateMedicamentoEstadoUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class UpdateMedicamentoEstadoService implements UpdateMedicamentoEstadoUseCase {

    private final MedicamentoRepository medicamentoRepository;
    private final MedicamentoEstadoRepository estadoRepository;

    @Inject
    public UpdateMedicamentoEstadoService(MedicamentoRepository medicamentoRepository,
                                          MedicamentoEstadoRepository estadoRepository) {
        this.medicamentoRepository = medicamentoRepository;
        this.estadoRepository = estadoRepository;
    }

    @Override
    @Transactional
    public MedicamentoWithEstado execute(UUID medicamentoId, String nuevoEstadoNombre) {
        MedicamentoWithEstado existing = medicamentoRepository.findById(medicamentoId)
                .orElseThrow(() -> new MedicamentoNotFoundException(medicamentoId));

        MedicamentoEstado nuevoEstado = estadoRepository.findByNombre(nuevoEstadoNombre)
                .orElseThrow(() -> new EstadoNotFoundException(nuevoEstadoNombre));

        Medicamento updated = existing.medicamento().withEstado(nuevoEstado.getId());
        Medicamento saved = medicamentoRepository.update(updated);
        return new MedicamentoWithEstado(saved, nuevoEstado);
    }
}
