package itesm.medsync.domain.solicitud.usecase;

import itesm.medsync.domain.solicitud.model.SolicitudAcceso;

public interface RechazarSolicitudUseCase {
    SolicitudAcceso execute(String token);
}
