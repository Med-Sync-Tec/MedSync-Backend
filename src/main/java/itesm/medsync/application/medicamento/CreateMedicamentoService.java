package itesm.medsync.application.medicamento;

import itesm.medsync.domain.medicamento.exception.DuplicateMedicamentoException;
import itesm.medsync.domain.medicamento.exception.EstadoNotFoundException;
import itesm.medsync.domain.medicamento.model.Medicamento;
import itesm.medsync.domain.medicamento.model.MedicamentoEstado;
import itesm.medsync.domain.medicamento.model.MedicamentoEstadoNames;
import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;
import itesm.medsync.domain.medicamento.repository.MedicamentoEstadoRepository;
import itesm.medsync.domain.medicamento.repository.MedicamentoRepository;
import itesm.medsync.domain.medicamento.usecase.CreateMedicamentoUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CreateMedicamentoService implements CreateMedicamentoUseCase {

    private static final String DEFAULT_ESTADO = MedicamentoEstadoNames.VIGENTE;

    private final MedicamentoRepository medicamentoRepository;
    private final MedicamentoEstadoRepository estadoRepository;

    @Inject
    public CreateMedicamentoService(MedicamentoRepository medicamentoRepository,
                                    MedicamentoEstadoRepository estadoRepository) {
        this.medicamentoRepository = medicamentoRepository;
        this.estadoRepository = estadoRepository;
    }

    @Override
    @Transactional
    public MedicamentoWithEstado execute(String nombre, String descripcion) {
        medicamentoRepository.findByNombre(nombre).ifPresent(existing -> {
            throw new DuplicateMedicamentoException(nombre);
        });

        MedicamentoEstado estado = estadoRepository.findByNombre(DEFAULT_ESTADO)
                .orElseThrow(() -> new EstadoNotFoundException(DEFAULT_ESTADO));

        Medicamento medicamento = Medicamento.create(nombre, estado.getId(), descripcion);
        Medicamento saved = medicamentoRepository.save(medicamento);
        return new MedicamentoWithEstado(saved, estado);
    }
}
