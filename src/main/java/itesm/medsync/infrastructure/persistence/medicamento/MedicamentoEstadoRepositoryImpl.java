package itesm.medsync.infrastructure.persistence.medicamento;

import itesm.medsync.domain.medicamento.model.MedicamentoEstado;
import itesm.medsync.domain.medicamento.repository.MedicamentoEstadoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class MedicamentoEstadoRepositoryImpl implements MedicamentoEstadoRepository {

    @Inject
    EntityManager em;

    @Override
    public Optional<MedicamentoEstado> findByNombre(String nombre) {
        return em.createQuery(
                        "SELECT e FROM MedicamentoEstadoEntity e WHERE e.nombre = :nombre",
                        MedicamentoEstadoEntity.class)
                .setParameter("nombre", nombre)
                .getResultStream()
                .findFirst()
                .map(MedicamentoPersistenceMapper::estadoToDomain);
    }

    @Override
    public Optional<MedicamentoEstado> findById(UUID id) {
        return Optional.ofNullable(em.find(MedicamentoEstadoEntity.class, id))
                .map(MedicamentoPersistenceMapper::estadoToDomain);
    }
}
