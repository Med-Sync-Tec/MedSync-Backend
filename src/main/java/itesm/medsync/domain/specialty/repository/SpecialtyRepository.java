package itesm.medsync.domain.specialty.repository;

import itesm.medsync.domain.specialty.model.Specialty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for the {@code specialty} feature.
 *
 * "Active" methods are filtered by {@code @SQLRestriction(activo = true)} on the entity;
 * "AcrossAllRows" methods bypass the filter and see soft-deleted rows as well — needed
 * to enforce slug uniqueness across the full table history.
 */
public interface SpecialtyRepository {

    /** Inserts or updates a specialty. Returns the persisted instance with timestamps populated. */
    Specialty save(Specialty specialty);

    /** Returns the active specialty with this id, or empty if none. Soft-deleted rows are invisible. */
    Optional<Specialty> findByUuid(UUID id);

    /** Returns the active specialty with this slug, or empty if none. */
    Optional<Specialty> findBySlug(String slug);

    /** Returns every active specialty, ordered alphabetically by {@code nombre}. */
    List<Specialty> findAllActive();

    /** True if an active specialty already uses this {@code nombre} (case-sensitive). */
    boolean existsByNombreActive(String nombre);

    /**
     * True if any row (active OR soft-deleted) ever used this slug.
     *
     * Slugs must be globally unique across history to avoid resurrecting a deleted
     * specialty's URL paths and vocabulary files under a different identity.
     */
    boolean existsBySlugAcrossAllRows(String slug);
}
