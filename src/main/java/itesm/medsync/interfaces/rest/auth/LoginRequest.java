package itesm.medsync.interfaces.rest.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @Size(max = 30)
    @Pattern(regexp = "^[A-Za-z_]{1,30}$",
            message = "expectedRole only allows letters and underscores")
    public String expectedRole;
}
