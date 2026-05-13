package itesm.medsync.domain.specialty.usecase;

import itesm.medsync.domain.specialty.model.Specialty;

import java.util.UUID;

public interface GetSpecialtyByIdUseCase {

    Specialty execute(UUID id);
}
