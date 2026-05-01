package itesm.medsync.application.medicamento;

import itesm.medsync.domain.medicamento.exception.DuplicateMedicamentoException;
import itesm.medsync.domain.medicamento.exception.EstadoNotFoundException;
import itesm.medsync.domain.medicamento.exception.MedicamentoNotFoundException;
import itesm.medsync.domain.medicamento.model.Medicamento;
import itesm.medsync.domain.medicamento.model.MedicamentoEstado;
import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;
import itesm.medsync.domain.medicamento.repository.MedicamentoEstadoRepository;
import itesm.medsync.domain.medicamento.repository.MedicamentoRepository;
import itesm.medsync.domain.medicamento.usecase.UpdateMedicamentoUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class UpdateMedicamentoService implements UpdateMedicamentoUseCase {

    private final MedicamentoRepository medicamentoRepository;
    private final MedicamentoEstadoRepository estadoRepository;

    @Inject
    public UpdateMedicamentoService(MedicamentoRepository medicamentoRepository,
                                    MedicamentoEstadoRepository estadoRepository) {
        this.medicamentoRepository = medicamentoRepository;
        this.estadoRepository = estadoRepository;
    }

    @Override
    @Transactional
    public MedicamentoWithEstado execute(UUID id, String nombre, String estadoNombre, String descripcion) {
        MedicamentoWithEstado existing = medicamentoRepository.findByUuid(id)
                .orElseThrow(() -> new MedicamentoNotFoundException(id));

        medicamentoRepository.findByNombre(nombre).ifPresent(found -> {
            if (!found.medicamento().getId().equals(id)) {
                throw new DuplicateMedicamentoException(nombre);
            }
        });

        MedicamentoEstado nuevoEstado = estadoRepository.findByNombre(estadoNombre)
                .orElseThrow(() -> new EstadoNotFoundException(estadoNombre));

        Medicamento updated = existing.medicamento().update(nombre, nuevoEstado.getId(), descripcion);
        medicamentoRepository.update(updated);
        return new MedicamentoWithEstado(updated, nuevoEstado);
    }
}
