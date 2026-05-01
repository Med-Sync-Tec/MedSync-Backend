package itesm.medsync.interfaces.rest.medicamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateMedicamentoRequest {

    @NotBlank
    @Size(max = 150)
    public String nombre;

    @NotBlank
    @Size(max = 50)
    public String estado;

    @Size(max = 1000)
    public String descripcion;
}
