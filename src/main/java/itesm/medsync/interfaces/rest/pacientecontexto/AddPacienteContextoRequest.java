package itesm.medsync.interfaces.rest.pacientecontexto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddPacienteContextoRequest {

    @NotBlank
    @Size(max = 30)
    public String tipo;

    @NotBlank
    @Size(max = 500)
    public String valor;
}
