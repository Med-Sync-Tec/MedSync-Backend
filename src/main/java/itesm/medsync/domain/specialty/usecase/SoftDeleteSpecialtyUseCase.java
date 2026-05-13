package itesm.medsync.domain.specialty.usecase;

import java.util.UUID;

public interface SoftDeleteSpecialtyUseCase {

    void execute(UUID id);
}
