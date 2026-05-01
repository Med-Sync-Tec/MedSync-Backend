package itesm.medsync.interfaces.rest.medicamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateMedicamentoRequest {

    @NotBlank
    @Size(max = 150)
    public String nombre;

    @Size(max = 1000)
    public String descripcion;
}
