package itesm.medsync.infrastructure.persistence.specialty;

import itesm.medsync.domain.specialty.model.Specialty;

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
