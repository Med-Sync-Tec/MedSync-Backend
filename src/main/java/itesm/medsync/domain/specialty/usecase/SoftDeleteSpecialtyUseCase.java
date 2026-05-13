package itesm.medsync.domain.specialty.usecase;

import java.util.UUID;

/**
 * Marks a specialty as inactive (COO operation). The row is preserved for audit;
 * the {@code @SQLRestriction} on the entity hides it from subsequent queries.
 *
 * Throws {@code SpecialtyNotFoundException} if the id does not resolve to an active row.
 */
public interface SoftDeleteSpecialtyUseCase {

    void execute(UUID id);
}
