package itesm.medsync.domain.hospital.model;

import itesm.medsync.domain.hospital.exception.InvalidHospitalDataException;

import java.time.LocalDateTime;
import java.util.Objects;

public final class ExpedienteClinico {

    private final String id;
    private final String pacienteExternoId;
    private final String doctorResponsableId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ExpedienteClinico(String id,
                             String pacienteExternoId,
                             String doctorResponsableId,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
        validate(id, pacienteExternoId);
        this.id = id;
        this.pacienteExternoId = pacienteExternoId;
        this.doctorResponsableId = doctorResponsableId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private static void validate(String id, String pacienteExternoId) {
        if (id == null || id.isBlank()) {
            throw new InvalidHospitalDataException("ExpedienteClinico id cannot be null or blank");
        }
        if (pacienteExternoId == null || pacienteExternoId.isBlank()) {
            throw new InvalidHospitalDataException("pacienteExternoId cannot be null or blank");
        }
    }

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
