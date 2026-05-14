package itesm.medsync.infrastructure.persistence.pacientecontexto;

import itesm.medsync.domain.shared.model.TipoClinico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "paciente_contexto")
public class PacienteContextoEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "paciente_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID pacienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoClinico tipo;

    @Column(nullable = false, length = 500)
    private String valor;

    @Column(name = "especialidad_id", columnDefinition = "BINARY(16)")
    private UUID especialidadId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public PacienteContextoEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(UUID pacienteId) {
        this.pacienteId = pacienteId;
    }

    public TipoClinico getTipo() {
        return tipo;
    }

    public void setTipo(TipoClinico tipo) {
        this.tipo = tipo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public UUID getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(UUID especialidadId) {
        this.especialidadId = especialidadId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
