package itesm.medsync.infrastructure.persistence.solicitud;

import itesm.medsync.domain.solicitud.model.EstadoSolicitud;
import itesm.medsync.domain.solicitud.model.SolicitudAcceso;

public final class SolicitudAccesoPersistenceMapper {

    private SolicitudAccesoPersistenceMapper() {}

    public static SolicitudAccesoEntity toEntity(SolicitudAcceso domain) {
        SolicitudAccesoEntity entity = new SolicitudAccesoEntity();
        entity.setId(domain.getId());
        entity.setNombre(domain.getNombre());
        entity.setCorreo(domain.getCorreo());
        entity.setRol(domain.getRol());
        entity.setEstado(domain.getEstado().name());
        entity.setToken(domain.getToken());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setRejectedAt(domain.getRejectedAt());
        return entity;
    }

    public static SolicitudAcceso toDomain(SolicitudAccesoEntity entity) {
        return new SolicitudAcceso(
                entity.getId(),
                entity.getNombre(),
                entity.getCorreo(),
                entity.getRol(),
                EstadoSolicitud.valueOf(entity.getEstado()),
                entity.getToken(),
                entity.getCreatedAt(),
                entity.getRejectedAt()
        );
    }
}
