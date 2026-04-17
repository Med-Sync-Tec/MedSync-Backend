package itesm.medsync.interfaces.rest.patient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String expedienteExternoId,
        String nombre,
        LocalDate fechaNacimiento,
        String genero,
        UUID medicoId,
        boolean activo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
