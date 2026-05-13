package itesm.medsync.domain.specialty.usecase;

import itesm.medsync.domain.specialty.model.Specialty;

import java.util.List;

public interface ListActiveSpecialtiesUseCase {

    List<Specialty> execute();
}
