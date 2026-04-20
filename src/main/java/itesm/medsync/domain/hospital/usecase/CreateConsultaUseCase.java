package itesm.medsync.domain.hospital.usecase;

import itesm.medsync.domain.hospital.model.Consulta;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CreateConsultaUseCase {

    Consulta execute(UUID patientId,
                     LocalDateTime fecha,
                     String motivoConsulta,
                     String subjetivo,
                     String objetivo,
                     String evaluacion,
                     String plan,
                     String prescripcion,
                     String diagnostico);
}
