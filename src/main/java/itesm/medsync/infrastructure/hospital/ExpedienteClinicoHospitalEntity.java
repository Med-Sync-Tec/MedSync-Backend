package itesm.medsync.infrastructure.hospital;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "expedientes_clinicos")
public class ExpedienteClinicoHospitalEntity {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "paciente_externo_id", nullable = false, unique = true, length = 100)
    private String pacienteExternoId;

    @Column(name = "doctor_responsable_id", length = 50)
    private String doctorResponsableId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ExpedienteClinicoHospitalEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPacienteExternoId() {
        return pacienteExternoId;
    }

    public void setPacienteExternoId(String pacienteExternoId) {
        this.pacienteExternoId = pacienteExternoId;
    }

    public String getDoctorResponsableId() {
        return doctorResponsableId;
    }

    public void setDoctorResponsableId(String doctorResponsableId) {
        this.doctorResponsableId = doctorResponsableId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
