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

/**
 * Persistence adapter for {@link SpecialtyRepository}. Implements both the domain
 * port and {@link PanacheRepositoryBase} in the same class, per the project's
 * "one repository file per feature" rule.
 */
@ApplicationScoped
@Transactional
public class SpecialtyRepositoryImpl
        implements SpecialtyRepository, PanacheRepositoryBase<SpecialtyEntity, UUID> {

    @Override
    public Specialty save(Specialty specialty) {
        // Update path: re-attach to the managed entity so Hibernate emits an UPDATE,
        // not a duplicate INSERT. Without this, save() of an existing aggregate would
        // try to insert a row that already exists.
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
        // Native query intentionally bypasses the @SQLRestriction filter so it can
        // see soft-deleted rows. A JPQL query would inherit the filter and miss
        // historic slugs, defeating the uniqueness-across-history guarantee.
        Object result = getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM especialidades WHERE slug = :slug")
                .setParameter("slug", slug)
                .getSingleResult();
        return ((Number) result).longValue() > 0L;
    }
}
