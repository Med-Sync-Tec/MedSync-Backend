package itesm.medsync.infrastructure.persistence.patient;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class PatientRepositoryImpl implements PatientRepository, PanacheRepositoryBase<PatientEntity, UUID> {

    @Override
    public Patient save(Patient patient) {
        Optional<PatientEntity> existing = findByIdOptional(patient.getId());
        if (existing.isPresent()) {
            PatientEntity managed = existing.get();
            PatientPersistenceMapper.copyInto(patient, managed);
            getEntityManager().flush();
            return PatientPersistenceMapper.toDomain(managed);
        }
        PatientEntity fresh = PatientPersistenceMapper.toEntity(patient);
        persist(fresh);
        flush();
        return PatientPersistenceMapper.toDomain(fresh);
    }

    @Override
    public Optional<Patient> findByUuid(UUID id) {
        return find("id", id).firstResultOptional()
                .map(PatientPersistenceMapper::toDomain);
    }

    @Override
    public List<Patient> findAllActive() {
        return listAll().stream()
                .map(PatientPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByExpedienteExternoId(String expedienteExternoId) {
        Object result = getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM patients WHERE expediente_externo_id = :exp")
                .setParameter("exp", expedienteExternoId)
                .getSingleResult();
        return ((Number) result).longValue() > 0L;
    }

    @Override
    public List<Patient> findMatchingPatientsForArticle(UUID articleId, int limit) {
        List<UUID> ids = getEntityManager().createQuery(
                "SELECT DISTINCT p.id FROM PatientEntity p " +
                        "WHERE EXISTS (" +
                        "  SELECT 1 FROM itesm.medsync.infrastructure.persistence.pacientecontexto.PacienteContextoEntity pc " +
                        "  WHERE pc.pacienteId = p.id " +
                        "    AND EXISTS (" +
                        "      SELECT 1 FROM itesm.medsync.infrastructure.persistence.article.ArticleEntity a JOIN a.tags at " +
                        "      WHERE a.id = :articleId " +
                        "        AND at.tipo = pc.tipo " +
                        "        AND at.valor = pc.valor " +
                        "        AND a.especialidadId = pc.especialidadId" +
                        "    )" +
                        ") ORDER BY p.id", UUID.class)
                .setParameter("articleId", articleId)
                .setMaxResults(limit)
                .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        java.util.Map<UUID, PatientEntity> byId = new java.util.HashMap<>();
        for (PatientEntity entity : getEntityManager()
                .createQuery("SELECT p FROM PatientEntity p WHERE p.id IN :ids", PatientEntity.class)
                .setParameter("ids", ids)
                .getResultList()) {
            byId.put(entity.getId(), entity);
        }

        return ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(PatientPersistenceMapper::toDomain)
                .toList();
    }
}
