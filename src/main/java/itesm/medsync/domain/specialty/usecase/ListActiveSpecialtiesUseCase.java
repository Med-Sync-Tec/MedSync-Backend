package itesm.medsync.domain.specialty.usecase;

import itesm.medsync.domain.specialty.model.Specialty;

import java.util.List;

/** Returns every active specialty, ordered alphabetically by {@code nombre}. Empty list if none. */
public interface ListActiveSpecialtiesUseCase {

    List<Specialty> execute();
}
