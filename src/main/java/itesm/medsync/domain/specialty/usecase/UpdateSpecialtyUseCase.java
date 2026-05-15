package itesm.medsync.domain.specialty.usecase;

import itesm.medsync.domain.specialty.model.Specialty;

import java.util.UUID;

/**
 * Replaces the editable fields of an existing specialty (COO operation).
 *
 * Throws {@code SpecialtyNotFoundException} if the id is unknown,
 * {@code DuplicateSpecialtyException} on a {@code nombre} or {@code slug} collision,
 * and {@code InvalidSpecialtyDataException} on invariant violations.
 */
public interface UpdateSpecialtyUseCase {

    Specialty execute(UUID id, String nombre, String slug, String descripcion);
}
