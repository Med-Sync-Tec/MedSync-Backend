package itesm.medsync.infrastructure.persistence.specialty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for the {@code especialidades} table.
 *
 * Soft-delete: the class-level {@link SQLRestriction} filters {@code activo = false}
 * rows from every read query — Hibernate appends the predicate automatically. To see
 * soft-deleted rows (e.g. for slug-uniqueness checks across history), bypass the
 * filter via a native query in {@link SpecialtyRepositoryImpl}.
 *
 * The {@code BINARY(16)} column definition lets Hibernate store {@link UUID} values
 * compactly on MySQL; H2 in {@code MODE=MySQL} accepts the same definition for dev.
 */
@Entity
@Table(name = "especialidades")
@SQLRestriction("activo = true")
public class SpecialtyEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SpecialtyEntity() {
        // Required by Hibernate
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
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
