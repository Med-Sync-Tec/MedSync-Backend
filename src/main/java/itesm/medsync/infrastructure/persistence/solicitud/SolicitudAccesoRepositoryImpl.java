package itesm.medsync.infrastructure.persistence.solicitud;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.solicitud.model.SolicitudAcceso;
import itesm.medsync.domain.solicitud.repository.SolicitudAccesoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class SolicitudAccesoRepositoryImpl implements SolicitudAccesoRepository,
        PanacheRepositoryBase<SolicitudAccesoEntity, UUID> {

    @Override
    public SolicitudAcceso save(SolicitudAcceso solicitud) {
        Optional<SolicitudAccesoEntity> existing = findByIdOptional(solicitud.getId());
        if (existing.isPresent()) {
            SolicitudAccesoEntity managed = existing.get();
            managed.setEstado(solicitud.getEstado().name());
            managed.setRejectedAt(solicitud.getRejectedAt());
            getEntityManager().flush();
            return SolicitudAccesoPersistenceMapper.toDomain(managed);
        }
        SolicitudAccesoEntity entity = SolicitudAccesoPersistenceMapper.toEntity(solicitud);
        persist(entity);
        flush();
        return SolicitudAccesoPersistenceMapper.toDomain(entity);
    }

    @Override
    public Optional<SolicitudAcceso> findByToken(String token) {
        return find("token", token).firstResultOptional()
                .map(SolicitudAccesoPersistenceMapper::toDomain);
    }

    @Override
    public Optional<SolicitudAcceso> findByCorreo(String correo) {
        return find("correo = ?1 AND estado IN ('PENDIENTE', 'RECHAZADO') ORDER BY createdAt DESC", correo)
                .firstResultOptional()
                .map(SolicitudAccesoPersistenceMapper::toDomain);
    }

    @Override
    public void delete(SolicitudAcceso solicitud) {
        findByIdOptional(solicitud.getId()).ifPresent(this::delete);
    }
}
