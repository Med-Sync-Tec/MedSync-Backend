package itesm.medsync.infrastructure.persistence.medicamento;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.medicamento.exception.EstadoNotFoundException;
import itesm.medsync.domain.medicamento.exception.MedicamentoNotFoundException;
import itesm.medsync.domain.medicamento.model.Medicamento;
import itesm.medsync.domain.medicamento.model.MedicamentosPage;
import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;
import itesm.medsync.domain.medicamento.repository.MedicamentoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class MedicamentoRepositoryImpl
        implements MedicamentoRepository, PanacheRepositoryBase<MedicamentoEntity, UUID> {

    private static final String ENTITY_GRAPH_WITH_ESTADO = "Medicamento.withEstado";
    private static final String FETCH_GRAPH_HINT = "jakarta.persistence.fetchgraph";

    @Inject
    EntityManager em;

    @Override
    public MedicamentosPage findPaginated(String nombre, String estado, int page, int size) {
        StringBuilder whereClause = new StringBuilder();
        List<Object[]> params = new ArrayList<>();

        if (nombre != null && !nombre.isBlank()) {
            whereClause.append(" AND LOWER(m.nombre) LIKE :nombre");
            params.add(new Object[]{"nombre", "%" + nombre.toLowerCase() + "%"});
        }
        if (estado != null && !estado.isBlank()) {
            whereClause.append(" AND m.estado.nombre = :estado");
            params.add(new Object[]{"estado", estado});
        }

        String base = "FROM MedicamentoEntity m"
                + (whereClause.isEmpty() ? "" : " WHERE 1=1" + whereClause);

        EntityGraph<?> graph = em.getEntityGraph(ENTITY_GRAPH_WITH_ESTADO);
        var dataQuery = em.createQuery("SELECT m " + base, MedicamentoEntity.class)
                .setHint(FETCH_GRAPH_HINT, graph)
                .setFirstResult(page * size)
                .setMaxResults(size);

        var countQuery = em.createQuery("SELECT COUNT(m) " + base, Long.class);

        for (Object[] param : params) {
            dataQuery.setParameter((String) param[0], param[1]);
            countQuery.setParameter((String) param[0], param[1]);
        }

        List<MedicamentoWithEstado> content = dataQuery.getResultList()
                .stream()
                .map(MedicamentoPersistenceMapper::toDomain)
                .toList();

        long total = countQuery.getSingleResult();
        int totalPages = (int) Math.ceil((double) total / size);

        return new MedicamentosPage(content, page, size, total, totalPages);
    }

    @Override
    public Optional<MedicamentoWithEstado> findByUuid(UUID id) {
        EntityGraph<?> graph = em.getEntityGraph(ENTITY_GRAPH_WITH_ESTADO);
        return em.createQuery("SELECT m FROM MedicamentoEntity m WHERE m.id = :id", MedicamentoEntity.class)
                .setParameter("id", id)
                .setHint(FETCH_GRAPH_HINT, graph)
                .getResultStream()
                .findFirst()
                .map(MedicamentoPersistenceMapper::toDomain);
    }

    @Override
    public Optional<MedicamentoWithEstado> findByNombre(String nombre) {
        EntityGraph<?> graph = em.getEntityGraph(ENTITY_GRAPH_WITH_ESTADO);
        return em.createQuery("SELECT m FROM MedicamentoEntity m WHERE m.nombre = :nombre", MedicamentoEntity.class)
                .setParameter("nombre", nombre)
                .setHint(FETCH_GRAPH_HINT, graph)
                .getResultStream()
                .findFirst()
                .map(MedicamentoPersistenceMapper::toDomain);
    }

    @Override
    public Medicamento save(Medicamento medicamento) {
        MedicamentoEstadoEntity estadoEntity = em.find(MedicamentoEstadoEntity.class, medicamento.getEstadoId());
        if (estadoEntity == null) {
            throw new EstadoNotFoundException(medicamento.getEstadoId().toString());
        }
        MedicamentoEntity entity = MedicamentoPersistenceMapper.toEntity(medicamento, estadoEntity);
        persist(entity);
        return medicamento;
    }

    @Override
    public Medicamento update(Medicamento medicamento) {
        MedicamentoEntity entity = findByIdOptional(medicamento.getId())
                .orElseThrow(() -> new MedicamentoNotFoundException(medicamento.getId()));
        MedicamentoEstadoEntity estadoEntity = em.find(MedicamentoEstadoEntity.class, medicamento.getEstadoId());
        if (estadoEntity == null) {
            throw new EstadoNotFoundException(medicamento.getEstadoId().toString());
        }
        entity.setNombre(medicamento.getNombre());
        entity.setDescripcion(medicamento.getDescripcion());
        entity.setEstado(estadoEntity);
        return medicamento;
    }

    @Override
    public void delete(UUID id) {
        deleteById(id);
    }
}
