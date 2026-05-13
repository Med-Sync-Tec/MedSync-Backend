package itesm.medsync.domain.specialty.usecase;

import itesm.medsync.domain.specialty.model.Specialty;

/**
 * Adds a new specialty to the catalog (COO operation).
 *
 * Throws {@code DuplicateSpecialtyException} if {@code nombre} or {@code slug} collides
 * with an existing entry, and {@code InvalidSpecialtyDataException} if any field violates
 * a domain invariant.
 */
public interface CreateSpecialtyUseCase {

    Specialty execute(String nombre, String slug, String descripcion);
}
