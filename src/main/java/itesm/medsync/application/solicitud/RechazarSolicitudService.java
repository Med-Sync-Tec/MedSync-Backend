package itesm.medsync.application.solicitud;

import itesm.medsync.domain.solicitud.exception.SolicitudNotFoundException;
import itesm.medsync.domain.solicitud.model.EstadoSolicitud;
import itesm.medsync.domain.solicitud.model.SolicitudAcceso;
import itesm.medsync.domain.solicitud.repository.SolicitudAccesoRepository;
import itesm.medsync.domain.solicitud.usecase.RechazarSolicitudUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class RechazarSolicitudService implements RechazarSolicitudUseCase {

    private final SolicitudAccesoRepository solicitudRepository;

    @Inject
    public RechazarSolicitudService(SolicitudAccesoRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    @Override
    @Transactional
    public SolicitudAcceso execute(String token) {
        SolicitudAcceso solicitud = solicitudRepository.findByToken(token)
                .orElseThrow(() -> new SolicitudNotFoundException(token));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BadRequestException("Esta solicitud ya fue procesada.");
        }

        solicitud.rechazar();
        return solicitudRepository.save(solicitud);
    }
}
