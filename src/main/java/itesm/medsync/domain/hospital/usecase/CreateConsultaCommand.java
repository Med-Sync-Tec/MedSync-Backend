package itesm.medsync.domain.hospital.usecase;

import java.time.LocalDateTime;

public record CreateConsultaCommand(
        LocalDateTime fecha,
        String motivoConsulta,
        String subjetivo,
        String objetivo,
        String evaluacion,
        String plan,
        String prescripcion,
        String diagnostico) {}
