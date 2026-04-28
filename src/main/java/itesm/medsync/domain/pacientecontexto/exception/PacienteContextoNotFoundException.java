package itesm.medsync.domain.pacientecontexto.exception;

import java.util.UUID;

public class PacienteContextoNotFoundException extends RuntimeException {

    public PacienteContextoNotFoundException(UUID id) {
        super("PacienteContexto not found: " + id);
    }
}
