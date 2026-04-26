package itesm.medsync.domain.patient.usecase;

import itesm.medsync.domain.patient.model.Patient;

import java.util.List;

public interface ListActivePatientsUseCase {

    List<Patient> execute();
}
