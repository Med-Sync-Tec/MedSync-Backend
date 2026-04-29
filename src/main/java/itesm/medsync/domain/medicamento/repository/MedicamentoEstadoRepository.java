package itesm.medsync.domain.medicamento.repository;

import itesm.medsync.domain.medicamento.model.MedicamentoEstado;

import java.util.Optional;
import java.util.UUID;

public interface MedicamentoEstadoRepository {
    Optional<MedicamentoEstado> findByNombre(String nombre);
    Optional<MedicamentoEstado> findById(UUID id);
}
