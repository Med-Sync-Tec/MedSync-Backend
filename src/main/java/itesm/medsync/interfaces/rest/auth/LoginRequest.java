package itesm.medsync.interfaces.rest.auth;

import jakarta.validation.constraints.Size;

public class LoginRequest {

    @Size(max = 30)
    public String expectedRole;
}
