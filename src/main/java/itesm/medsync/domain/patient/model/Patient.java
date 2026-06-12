package itesm.medsync.domain.patient.model;

import itesm.medsync.domain.patient.exception.InvalidPatientDataException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public final class Patient {

    private static final int MAX_AGE_YEARS = 150;

    private final UUID id;
    private final String expedienteExternoId;
    private final String nombre;
    private final LocalDate fechaNacimiento;
    private final String genero;
    private final UUID medicoId;
    private final boolean activo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Patient(UUID id,
                   String expedienteExternoId,
                   String nombre,
                   LocalDate fechaNacimiento,
                   String genero,
                   UUID medicoId,
                   boolean activo,
                   LocalDateTime createdAt,
                   LocalDateTime updatedAt) {
        validate(id, expedienteExternoId, nombre, fechaNacimiento, medicoId);
        this.id = id;
        this.expedienteExternoId = expedienteExternoId;
        this.nombre = nombre;
        this.fechaNacimiento = fechaNacimiento;
        this.genero = genero;
        this.medicoId = medicoId;
        this.activo = activo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Patient create(String expedienteExternoId,
                                 String nombre,
                                 LocalDate fechaNacimiento,
                                 String genero,
                                 UUID medicoId) {
        return new Patient(
                UUID.randomUUID(),
                expedienteExternoId,
                nombre,
                fechaNacimiento,
                genero,
                medicoId,
                true,
                null,
                null);
    }

    public Patient softDelete() {
        return new Patient(
                id, expedienteExternoId, nombre, fechaNacimiento,
                genero, medicoId, false, createdAt, updatedAt);
    }

    private static void validate(UUID id,
                                 String expedienteExternoId,
                                 String nombre,
                                 LocalDate fechaNacimiento,
                                 UUID medicoId) {
        if (id == null) {
            throw new InvalidPatientDataException("Patient id cannot be null");
        }
        if (expedienteExternoId == null || expedienteExternoId.isBlank()) {
            throw new InvalidPatientDataException("expedienteExternoId cannot be null or blank");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new InvalidPatientDataException("nombre cannot be null or blank");
        }
        if (fechaNacimiento == null) {
            throw new InvalidPatientDataException("fechaNacimiento cannot be null");
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (fechaNacimiento.isAfter(today)) {
            throw new InvalidPatientDataException("fechaNacimiento cannot be in the future");
        }
        long years = ChronoUnit.YEARS.between(fechaNacimiento, today);
        if (years > MAX_AGE_YEARS) {
            throw new InvalidPatientDataException(
                    "fechaNacimiento implies an age greater than " + MAX_AGE_YEARS + " years");
        }
        if (medicoId == null) {
            throw new InvalidPatientDataException("medicoId cannot be null");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getExpedienteExternoId() {
        return expedienteExternoId;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getGenero() {
        return genero;
    }

    public UUID getMedicoId() {
        return medicoId;
    }

    public boolean isActivo() {
        return activo;
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
        if (!(o instanceof Patient other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
