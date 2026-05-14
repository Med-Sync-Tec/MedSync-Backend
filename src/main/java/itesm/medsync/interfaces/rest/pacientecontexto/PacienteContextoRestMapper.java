package itesm.medsync.interfaces.rest.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;

public final class PacienteContextoRestMapper {

    private PacienteContextoRestMapper() {
    }

    public static PacienteContextoResponse toResponse(PacienteContexto contexto) {
        return new PacienteContextoResponse(
                contexto.getId(),
                contexto.getPacienteId(),
                contexto.getTipo().name().toLowerCase(),
                contexto.getValor(),
                contexto.getEspecialidadId(),
                contexto.getCreatedAt()
        );
    }
}
