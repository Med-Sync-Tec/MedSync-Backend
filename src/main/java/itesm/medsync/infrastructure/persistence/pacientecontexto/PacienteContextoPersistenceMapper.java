package itesm.medsync.infrastructure.persistence.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;

public final class PacienteContextoPersistenceMapper {

    private PacienteContextoPersistenceMapper() {
    }

    public static PacienteContextoEntity toEntity(PacienteContexto contexto) {
        PacienteContextoEntity entity = new PacienteContextoEntity();
        entity.setId(contexto.getId());
        entity.setPacienteId(contexto.getPacienteId());
        entity.setTipo(contexto.getTipo());
        entity.setValor(contexto.getValor());
        entity.setEspecialidadId(contexto.getEspecialidadId());
        entity.setCreatedAt(contexto.getCreatedAt());
        return entity;
    }

    public static PacienteContexto toDomain(PacienteContextoEntity entity) {
        return new PacienteContexto(
                entity.getId(),
                entity.getPacienteId(),
                entity.getTipo(),
                entity.getValor(),
                entity.getEspecialidadId(),
                entity.getCreatedAt());
    }
}
