package itesm.medsync.domain.specialty.usecase;

import itesm.medsync.domain.specialty.model.Specialty;

import java.util.UUID;

/**
 * Looks up an active specialty by id.
 *
 * Throws {@code SpecialtyNotFoundException} if the id does not resolve to an active row.
 */
public interface GetSpecialtyByIdUseCase {

    Specialty execute(UUID id);
}
