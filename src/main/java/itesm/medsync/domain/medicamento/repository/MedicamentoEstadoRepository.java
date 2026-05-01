package itesm.medsync.domain.medicamento.repository;

import itesm.medsync.domain.medicamento.model.MedicamentoEstado;

import java.util.Optional;

public interface MedicamentoEstadoRepository {
    Optional<MedicamentoEstado> findByNombre(String nombre);
}
