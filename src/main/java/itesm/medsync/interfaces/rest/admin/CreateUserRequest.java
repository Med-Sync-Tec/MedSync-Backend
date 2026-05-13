package itesm.medsync.interfaces.rest.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateUserRequest {

    @NotBlank
    @Email
    @Size(max = 100)
    public String correo;

    @NotBlank
    @Size(max = 100)
    public String nombre;

    @NotBlank
    @Size(min = 6, max = 100)
    public String password;

    @NotBlank
    @Size(max = 30)
    public String rol;

    public UUID especialidadId;
}
