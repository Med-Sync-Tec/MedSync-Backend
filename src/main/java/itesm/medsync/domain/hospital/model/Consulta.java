package itesm.medsync.domain.hospital.model;

import itesm.medsync.domain.hospital.exception.InvalidHospitalDataException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class Consulta {

    private final String id;
    private final String expedienteId;
    private final LocalDateTime fecha;
    private final String motivoConsulta;
    private final String subjetivo;
    private final String objetivo;
    private final String evaluacion;
    private final String plan;
    private final String prescripcion;
    private final String diagnostico;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Consulta(String id,
                    String expedienteId,
                    LocalDateTime fecha,
                    String motivoConsulta,
                    String subjetivo,
                    String objetivo,
                    String evaluacion,
                    String plan,
                    String prescripcion,
                    String diagnostico,
                    LocalDateTime createdAt,
                    LocalDateTime updatedAt) {
        validate(id, expedienteId, fecha);
        this.id = id;
        this.expedienteId = expedienteId;
        this.fecha = fecha;
        this.motivoConsulta = motivoConsulta;
        this.subjetivo = subjetivo;
        this.objetivo = objetivo;
        this.evaluacion = evaluacion;
        this.plan = plan;
        this.prescripcion = prescripcion;
        this.diagnostico = diagnostico;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Consulta create(String expedienteId,
                                  LocalDateTime fecha,
                                  String motivoConsulta,
                                  String subjetivo,
                                  String objetivo,
                                  String evaluacion,
                                  String plan,
                                  String prescripcion,
                                  String diagnostico) {
        return new Consulta(
                UUID.randomUUID().toString(),
                expedienteId,
                fecha,
                motivoConsulta,
                subjetivo,
                objetivo,
                evaluacion,
                plan,
                prescripcion,
                diagnostico,
                null,
                null);
    }

    private static void validate(String id, String expedienteId, LocalDateTime fecha) {
        if (id == null || id.isBlank()) {
            throw new InvalidHospitalDataException("Consulta id cannot be null or blank");
        }
        if (expedienteId == null || expedienteId.isBlank()) {
            throw new InvalidHospitalDataException("expedienteId cannot be null or blank");
        }
        if (fecha == null) {
            throw new InvalidHospitalDataException("fecha cannot be null");
        }
    }

    public String getId() {
        return id;
    }

    public String getExpedienteId() {
        return expedienteId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public String getSubjetivo() {
        return subjetivo;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public String getEvaluacion() {
        return evaluacion;
    }

    public String getPlan() {
        return plan;
    }

    public String getPrescripcion() {
        return prescripcion;
    }

    public String getDiagnostico() {
        return diagnostico;
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
        if (!(o instanceof Consulta other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
