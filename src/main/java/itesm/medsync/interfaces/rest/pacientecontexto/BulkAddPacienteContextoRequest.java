package itesm.medsync.interfaces.rest.pacientecontexto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body of {@code POST /api/patients/{patientId}/contextos/bulk}. The 50-entry
 * cap on {@code entries} is a pragmatic safeguard against accidental DoS via
 * a misconfigured frontend; AI analyze rarely returns more than ~10–20
 * suggestions in practice.
 */
public class BulkAddPacienteContextoRequest {

    @NotEmpty
    @Size(max = 50)
    @Valid
    public List<EntryDto> entries;

    public static class EntryDto {

        @NotBlank
        @Size(max = 30)
        public String tipo;

        @NotBlank
        @Size(max = 500)
        public String valor;
    }
}
