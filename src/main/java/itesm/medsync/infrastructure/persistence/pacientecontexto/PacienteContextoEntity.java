package itesm.medsync.infrastructure.persistence.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
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
    private PacienteContexto.Tipo tipo;

    @Column(nullable = false, length = 500)
    private String valor;

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

    public PacienteContexto.Tipo getTipo() {
        return tipo;
    }

    public void setTipo(PacienteContexto.Tipo tipo) {
        this.tipo = tipo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
