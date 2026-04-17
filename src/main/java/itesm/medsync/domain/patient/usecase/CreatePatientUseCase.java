package itesm.medsync.domain.patient.usecase;

import itesm.medsync.domain.patient.model.Patient;

import java.time.LocalDate;
import java.util.UUID;

public interface CreatePatientUseCase {

    Patient execute(String expedienteExternoId,
                    String nombre,
                    LocalDate fechaNacimiento,
                    String genero,
                    UUID medicoId);
}
