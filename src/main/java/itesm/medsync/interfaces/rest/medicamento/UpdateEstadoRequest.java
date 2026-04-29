package itesm.medsync.interfaces.rest.medicamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateEstadoRequest {

    @NotBlank
    @Size(max = 50)
    public String estado;
}
