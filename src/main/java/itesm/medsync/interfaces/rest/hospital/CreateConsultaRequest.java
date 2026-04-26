package itesm.medsync.interfaces.rest.hospital;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CreateConsultaRequest {

    @NotNull
    public LocalDateTime fecha;

    @Size(max = 2000)
    public String motivoConsulta;

    @Size(max = 5000)
    public String subjetivo;

    @Size(max = 5000)
    public String objetivo;

    @Size(max = 5000)
    public String evaluacion;

    @Size(max = 5000)
    public String plan;

    @Size(max = 5000)
    public String prescripcion;

    @Size(max = 2000)
    public String diagnostico;
}
