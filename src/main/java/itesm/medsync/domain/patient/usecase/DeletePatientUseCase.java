package itesm.medsync.domain.patient.usecase;

import java.util.UUID;

public interface DeletePatientUseCase {

    void execute(UUID id);
}
