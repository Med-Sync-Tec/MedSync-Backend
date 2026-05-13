package itesm.medsync.interfaces.rest.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    @Size(min = 6, max = 100)
    public String password;

    @NotBlank
    @Size(max = 30)
    public String rol;
}
