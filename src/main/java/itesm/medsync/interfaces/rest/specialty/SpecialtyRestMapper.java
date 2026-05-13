package itesm.medsync.interfaces.rest.specialty;

import itesm.medsync.domain.specialty.model.Specialty;

import java.util.List;

public final class SpecialtyRestMapper {

    private SpecialtyRestMapper() {
    }

    public static SpecialtyResponse toResponse(Specialty s) {
        return new SpecialtyResponse(
                s.getId(),
                s.getNombre(),
                s.getSlug(),
                s.getDescripcion(),
                s.isActivo(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }

    public static List<SpecialtyResponse> toResponseList(List<Specialty> list) {
        return list.stream().map(SpecialtyRestMapper::toResponse).toList();
    }
}
