package itesm.medsync.infrastructure.persistence.specialty;

import itesm.medsync.domain.specialty.model.Specialty;

/** Translates between the {@link Specialty} domain aggregate and the JPA {@link SpecialtyEntity}. */
public final class SpecialtyPersistenceMapper {

    private SpecialtyPersistenceMapper() {
    }

    public static SpecialtyEntity toEntity(Specialty specialty) {
        SpecialtyEntity entity = new SpecialtyEntity();
        entity.setId(specialty.getId());
        entity.setNombre(specialty.getNombre());
        entity.setSlug(specialty.getSlug());
        entity.setDescripcion(specialty.getDescripcion());
        entity.setActivo(specialty.isActivo());
        entity.setCreatedAt(specialty.getCreatedAt());
        entity.setUpdatedAt(specialty.getUpdatedAt());
        return entity;
    }

    /**
     * Copies editable fields from a domain aggregate onto a managed entity, so Hibernate
     * detects the change and issues an UPDATE on flush. Timestamps and id are skipped
     * because they are owned by the persistence layer.
     */
    public static void copyInto(Specialty specialty, SpecialtyEntity entity) {
        entity.setNombre(specialty.getNombre());
        entity.setSlug(specialty.getSlug());
        entity.setDescripcion(specialty.getDescripcion());
        entity.setActivo(specialty.isActivo());
    }

    public static Specialty toDomain(SpecialtyEntity entity) {
        return new Specialty(
                entity.getId(),
                entity.getNombre(),
                entity.getSlug(),
                entity.getDescripcion(),
                entity.isActivo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
