package itesm.medsync.interfaces.rest.patient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public class CreatePatientRequest {

    @NotBlank
    @Size(max = 100)
    public String expedienteExternoId;

    @NotBlank
    @Size(max = 200)
    public String nombre;

    @NotNull
    @Past
    public LocalDate fechaNacimiento;

    @Size(max = 20)
    public String genero;

    @NotNull
    public UUID medicoId;
}
