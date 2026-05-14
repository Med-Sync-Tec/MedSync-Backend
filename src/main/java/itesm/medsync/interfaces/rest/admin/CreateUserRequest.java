package itesm.medsync.interfaces.rest.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank
    @Email
    @Size(max = 100)
    public String correo;

    @NotBlank
    @Size(max = 100)
    public String nombre;

    // Password supplied by the COO — passed to Firebase, never stored in the database.
    @NotBlank
    @Size(min = 6, max = 100)
    public String password;

    @NotBlank
    @Pattern(regexp = "DOCTOR|COO|CMO", message = "Role must be DOCTOR, COO, or CMO")
    public String rol;
}
