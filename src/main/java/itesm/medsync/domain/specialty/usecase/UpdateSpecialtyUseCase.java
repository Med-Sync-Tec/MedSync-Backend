package itesm.medsync.domain.specialty.usecase;

import itesm.medsync.domain.specialty.model.Specialty;

import java.util.UUID;

public interface UpdateSpecialtyUseCase {

    Specialty execute(UUID id, String nombre, String slug, String descripcion);
}
