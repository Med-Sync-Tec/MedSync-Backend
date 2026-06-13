package itesm.medsync.domain.hospital.model;

import itesm.medsync.domain.hospital.exception.InvalidHospitalDataException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record ExpedienteClinico(
        String id,
        String pacienteExternoId,
        String doctorResponsableId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    // Compact constructor handles all validation
    public ExpedienteClinico {
        if (id == null || id.isBlank()) {
            throw new InvalidHospitalDataException("ExpedienteClinico id cannot be null or blank");
        }
        if (pacienteExternoId == null || pacienteExternoId.isBlank()) {
            throw new InvalidHospitalDataException("pacienteExternoId cannot be null or blank");
        }
    }

    public static ExpedienteClinico create(String pacienteExternoId, String doctorResponsableId) {
        return new ExpedienteClinico(
                UUID.randomUUID().toString(),
                pacienteExternoId,
                doctorResponsableId,
                null,
                null);
    }

    // Backward-compatible accessors
    public String getId() {
        return id;
    }

    public String getPacienteExternoId() {
        return pacienteExternoId;
    }

    public String getDoctorResponsableId() {
        return doctorResponsableId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Id-based equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpedienteClinico other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
