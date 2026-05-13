package itesm.medsync.infrastructure.persistence.specialty;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class SpecialtyRepositoryImpl
        implements SpecialtyRepository, PanacheRepositoryBase<SpecialtyEntity, UUID> {

    @Override
    public Specialty save(Specialty specialty) {
        Optional<SpecialtyEntity> existing = findByIdOptional(specialty.getId());
        if (existing.isPresent()) {
            SpecialtyEntity managed = existing.get();
            SpecialtyPersistenceMapper.copyInto(specialty, managed);
            getEntityManager().flush();
            return SpecialtyPersistenceMapper.toDomain(managed);
        }
        SpecialtyEntity fresh = SpecialtyPersistenceMapper.toEntity(specialty);
        persist(fresh);
        flush();
        return SpecialtyPersistenceMapper.toDomain(fresh);
    }

    @Override
    public Optional<Specialty> findByUuid(UUID id) {
        return find("id", id).firstResultOptional()
                .map(SpecialtyPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Specialty> findBySlug(String slug) {
        return find("slug", slug).firstResultOptional()
                .map(SpecialtyPersistenceMapper::toDomain);
    }

    @Override
    public List<Specialty> findAllActive() {
        return listAll(Sort.by("nombre")).stream()
                .map(SpecialtyPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNombreActive(String nombre) {
        return count("nombre = ?1 and activo = true", nombre) > 0L;
    }

    @Override
    public boolean existsBySlugAcrossAllRows(String slug) {
        Object result = getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM especialidades WHERE slug = :slug")
                .setParameter("slug", slug)
                .getSingleResult();
        return ((Number) result).longValue() > 0L;
    }
}
