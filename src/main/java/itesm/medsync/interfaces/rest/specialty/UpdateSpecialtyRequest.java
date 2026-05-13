package itesm.medsync.interfaces.rest.specialty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Inbound DTO for {@code PUT /api/admin/especialidades/{id}}. Same shape as create — all fields are replaced. */
public class UpdateSpecialtyRequest {

    @NotBlank
    @Size(max = 100)
    public String nombre;

    @NotBlank
    @Size(max = 60)
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
            message = "slug must be kebab-case (lowercase a-z, 0-9, single hyphens)")
    public String slug;

    @Size(max = 500)
    public String descripcion;
}
