package itesm.medsync.domain.patient.usecase;

import itesm.medsync.domain.patient.model.Patient;

import java.util.List;
import java.util.UUID;

public interface GetMatchingPatientsByArticleUseCase {
    List<Patient> execute(UUID articleId, int limit);
}
