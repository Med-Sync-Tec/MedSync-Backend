package itesm.medsync.domain.specialty.model;

import itesm.medsync.domain.specialty.exception.InvalidSpecialtyDataException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A medical specialty in the MedSync catalog (Cardiología, Pediatría, ...).
 *
 * Immutable: every mutating operation returns a new instance. All invariants
 * (length limits, kebab-case slug pattern, non-blank nombre) are enforced in
 * the compact constructor, so any {@code Specialty} instance is guaranteed to be valid.
 *
 * The {@code slug} is the stable identifier used by cross-feature artifacts —
 * notably vocabulary file names (see {@code resources/vocabulary/<slug>.json}).
 * Renaming a {@code nombre} is safe; renaming a {@code slug} breaks those links.
 */
public record Specialty(
        UUID id,
        String nombre,
        String slug,
        String descripcion,
        boolean activo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static final int MAX_NOMBRE_LENGTH = 100;
    public static final int MAX_SLUG_LENGTH = 60;
    public static final int MAX_DESCRIPCION_LENGTH = 500;
    // Kebab-case: lowercase a-z / 0-9, single hyphens, no leading/trailing hyphen.
    // Constrains slugs to safe URL and filename characters.
    public static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*+$");

    // Compact constructor handles all validation
    public Specialty {
        if (id == null) {
            throw new InvalidSpecialtyDataException("id cannot be null");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new InvalidSpecialtyDataException("nombre cannot be null or blank");
        }
        if (nombre.length() > MAX_NOMBRE_LENGTH) {
            throw new InvalidSpecialtyDataException(
                    "nombre cannot exceed " + MAX_NOMBRE_LENGTH + " characters");
        }
        if (slug == null || slug.isBlank()) {
            throw new InvalidSpecialtyDataException("slug cannot be null or blank");
        }
        if (slug.length() > MAX_SLUG_LENGTH) {
            throw new InvalidSpecialtyDataException(
                    "slug cannot exceed " + MAX_SLUG_LENGTH + " characters");
        }
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new InvalidSpecialtyDataException(
                    "slug must be kebab-case (lowercase a-z, 0-9, single hyphens): " + slug);
        }
        if (descripcion != null && descripcion.length() > MAX_DESCRIPCION_LENGTH) {
            throw new InvalidSpecialtyDataException(
                    "descripcion cannot exceed " + MAX_DESCRIPCION_LENGTH + " characters");
        }
    }

    /**
     * Builds a fresh {@code Specialty} with a random UUID, {@code activo=true},
     * and null timestamps (the persistence layer fills timestamps on insert).
     */
    public static Specialty create(String nombre, String slug, String descripcion) {
        return new Specialty(UUID.randomUUID(), nombre, slug, descripcion, true, null, null);
    }

    /** Returns a copy with {@code activo=false}. The original instance is unchanged. */
    public Specialty softDelete() {
        return new Specialty(id, nombre, slug, descripcion, false, createdAt, updatedAt);
    }

    /** Returns a copy with a new {@code nombre}; re-validates length and non-blank. */
    public Specialty withNombre(String newNombre) {
        return new Specialty(id, newNombre, slug, descripcion, activo, createdAt, updatedAt);
    }

    /** Returns a copy with a new {@code slug}; re-validates kebab-case pattern. */
    public Specialty withSlug(String newSlug) {
        return new Specialty(id, nombre, newSlug, descripcion, activo, createdAt, updatedAt);
    }

    /** Returns a copy with a new {@code descripcion}; null is allowed. */
    public Specialty withDescripcion(String newDescripcion) {
        return new Specialty(id, nombre, slug, newDescripcion, activo, createdAt, updatedAt);
    }

    // Backward-compatible accessors
    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescripcion() {
        return descripcion;
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

    // Identity is the UUID: two Specialty instances with the same id are equal
    // even if other fields differ (e.g. one is the soft-deleted version).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Specialty other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
