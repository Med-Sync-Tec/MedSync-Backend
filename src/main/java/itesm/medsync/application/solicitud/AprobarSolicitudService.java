package itesm.medsync.application.solicitud;

import itesm.medsync.domain.solicitud.exception.SolicitudNotFoundException;
import itesm.medsync.domain.solicitud.model.AprobarSolicitudResult;
import itesm.medsync.domain.solicitud.model.EstadoSolicitud;
import itesm.medsync.domain.solicitud.model.SolicitudAcceso;
import itesm.medsync.domain.solicitud.repository.SolicitudAccesoRepository;
import itesm.medsync.domain.solicitud.usecase.AprobarSolicitudUseCase;
import itesm.medsync.domain.user.model.ProvisionApprovedUserResult;
import itesm.medsync.domain.user.usecase.ProvisionApprovedUserUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class AprobarSolicitudService implements AprobarSolicitudUseCase {

    private final SolicitudAccesoRepository solicitudRepository;
    // Uses ProvisionApprovedUserUseCase — not CreateAdminUserUseCase — because this flow
    // must never receive a caller-supplied password; the password is generated internally.
    private final ProvisionApprovedUserUseCase provisionUser;

    @Inject
    public AprobarSolicitudService(SolicitudAccesoRepository solicitudRepository,
                                    ProvisionApprovedUserUseCase provisionUser) {
        this.solicitudRepository = solicitudRepository;
        this.provisionUser = provisionUser;
    }

    @Override
    @Transactional
    public AprobarSolicitudResult execute(String token) {
        SolicitudAcceso solicitud = solicitudRepository.findByToken(token)
                .orElseThrow(() -> new SolicitudNotFoundException(token));

        // Guard against double-click or replayed approve links.
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new BadRequestException("Esta solicitud ya fue procesada.");
        }

        ProvisionApprovedUserResult provisioned = provisionUser.execute(
                solicitud.getCorreo(),
                solicitud.getNombre(),
                solicitud.getRol()
        );

        solicitud.aprobar();
        solicitudRepository.save(solicitud);

        return new AprobarSolicitudResult(provisioned.user(), provisioned.passwordResetLink());
    }
}
