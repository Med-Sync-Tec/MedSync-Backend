package itesm.medsync.domain.medicamento.repository;

import itesm.medsync.domain.medicamento.model.Medicamento;
import itesm.medsync.domain.medicamento.model.MedicamentosPage;
import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicamentoRepository {
    List<MedicamentoWithEstado> findAll();
    MedicamentosPage findPaginated(String nombre, String estado, int page, int size);
    Optional<MedicamentoWithEstado> findById(UUID id);
    Optional<MedicamentoWithEstado> findByNombre(String nombre);
    Medicamento save(Medicamento medicamento);
    Medicamento update(Medicamento medicamento);
    void delete(UUID id);
}
