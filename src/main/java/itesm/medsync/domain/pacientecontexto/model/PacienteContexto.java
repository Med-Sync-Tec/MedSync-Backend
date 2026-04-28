package itesm.medsync.domain.pacientecontexto.model;

import itesm.medsync.domain.pacientecontexto.exception.InvalidPacienteContextoDataException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class PacienteContexto {

    private static final int MAX_VALOR_LENGTH = 500;

    private final UUID id;
    private final UUID pacienteId;
    private final Tipo tipo;
    private final String valor;
    private final LocalDateTime createdAt;

    public PacienteContexto(UUID id,
                            UUID pacienteId,
                            Tipo tipo,
                            String valor,
                            LocalDateTime createdAt) {
        validate(id, pacienteId, tipo, valor);
        this.id = id;
        this.pacienteId = pacienteId;
        this.tipo = tipo;
        this.valor = valor.trim();
        this.createdAt = createdAt;
    }

    public static PacienteContexto create(UUID pacienteId, Tipo tipo, String valor) {
        return new PacienteContexto(
                UUID.randomUUID(),
                pacienteId,
                tipo,
                valor,
                null);
    }

    private static void validate(UUID id, UUID pacienteId, Tipo tipo, String valor) {
        if (id == null) {
            throw new InvalidPacienteContextoDataException("id cannot be null");
        }
        if (pacienteId == null) {
            throw new InvalidPacienteContextoDataException("pacienteId cannot be null");
        }
        if (tipo == null) {
            throw new InvalidPacienteContextoDataException("tipo cannot be null");
        }
        if (valor == null || valor.isBlank()) {
            throw new InvalidPacienteContextoDataException("valor cannot be null or blank");
        }
        if (valor.length() > MAX_VALOR_LENGTH) {
            throw new InvalidPacienteContextoDataException(
                    "valor cannot exceed " + MAX_VALOR_LENGTH + " characters");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getValor() {
        return valor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PacienteContexto other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public enum Tipo {
        ENFERMEDAD,
        SINTOMA,
        TRATAMIENTO,
        MEDICAMENTO;

        public static Tipo fromString(String raw) {
            if (raw == null || raw.isBlank()) {
                throw new InvalidPacienteContextoDataException("tipo cannot be null or blank");
            }
            try {
                return Tipo.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new InvalidPacienteContextoDataException(
                        "tipo inválido: '" + raw + "'. Permitidos: enfermedad, sintoma, tratamiento, medicamento");
            }
        }
    }
}
