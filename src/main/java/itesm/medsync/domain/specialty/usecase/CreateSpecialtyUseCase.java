package itesm.medsync.domain.specialty.usecase;

import itesm.medsync.domain.specialty.model.Specialty;

public interface CreateSpecialtyUseCase {

    Specialty execute(String nombre, String slug, String descripcion);
}
