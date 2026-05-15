package itesm.medsync.domain.solicitud.usecase;

import itesm.medsync.domain.solicitud.model.SolicitudAcceso;

public interface CrearSolicitudUseCase {
    SolicitudAcceso execute(String nombre, String correo, String rol);
}
