package itesm.medsync.infrastructure.persistence.patient;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class PatientRepositoryImpl implements PatientRepository {

    private final PatientPanacheRepository panache;

    @Inject
    public PatientRepositoryImpl(PatientPanacheRepository panache) {
        this.panache = panache;
    }

    @Override
    public Patient save(Patient patient) {
        Optional<PatientEntity> existing = panache.findByIdOptional(patient.getId());
        if (existing.isPresent()) {
            PatientEntity managed = existing.get();
            PatientPersistenceMapper.copyInto(patient, managed);
            panache.getEntityManager().flush();
            return PatientPersistenceMapper.toDomain(managed);
        }
        PatientEntity fresh = PatientPersistenceMapper.toEntity(patient);
        panache.persist(fresh);
        panache.flush();
        return PatientPersistenceMapper.toDomain(fresh);
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        return panache.find("id", id).firstResultOptional()
                .map(PatientPersistenceMapper::toDomain);
    }

    @Override
    public List<Patient> findAllActive() {
        return panache.listAll().stream()
                .map(PatientPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByExpedienteExternoId(String expedienteExternoId) {
        Object result = panache.getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM patients WHERE expediente_externo_id = :exp")
                .setParameter("exp", expedienteExternoId)
                .getSingleResult();
        return ((Number) result).longValue() > 0L;
    }

    @ApplicationScoped
    public static class PatientPanacheRepository implements PanacheRepositoryBase<PatientEntity, UUID> {
    }
}
