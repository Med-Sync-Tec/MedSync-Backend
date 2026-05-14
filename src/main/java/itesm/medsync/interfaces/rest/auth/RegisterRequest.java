package itesm.medsync.interfaces.rest.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank
    @Size(max = 100)
    public String nombre;

    @NotBlank
    @Email
    @Size(max = 100)
    public String correo;

    @NotBlank
    @Pattern(regexp = "DOCTOR|COO", message = "Rol debe ser DOCTOR o COO")
    public String rol;
}
