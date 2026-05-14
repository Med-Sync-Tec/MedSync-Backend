package itesm.medsync.domain.solicitud.repository;

import itesm.medsync.domain.solicitud.model.SolicitudAcceso;

import java.util.Optional;

public interface SolicitudAccesoRepository {
    SolicitudAcceso save(SolicitudAcceso solicitud);
    Optional<SolicitudAcceso> findByToken(String token);
    Optional<SolicitudAcceso> findByCorreo(String correo);
    void delete(SolicitudAcceso solicitud);
}
