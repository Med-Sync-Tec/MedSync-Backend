package itesm.medsync.application.solicitud;

import itesm.medsync.domain.solicitud.exception.SolicitudPendienteException;
import itesm.medsync.domain.solicitud.exception.SolicitudRechazadaRecientementeException;
import itesm.medsync.domain.solicitud.model.EstadoSolicitud;
import itesm.medsync.domain.solicitud.model.SolicitudAcceso;
import itesm.medsync.domain.solicitud.repository.SolicitudAccesoRepository;
import itesm.medsync.domain.solicitud.usecase.CrearSolicitudUseCase;
import itesm.medsync.domain.user.exception.UserAlreadyExistsException;
import itesm.medsync.domain.user.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;

@ApplicationScoped
public class CrearSolicitudService implements CrearSolicitudUseCase {

    private final SolicitudAccesoRepository solicitudRepository;
    private final UserRepository userRepository;

    @ConfigProperty(name = "medsync.solicitudes.rejection-cooldown-days", defaultValue = "5")
    int cooldownDays;

    @Inject
    public CrearSolicitudService(SolicitudAccesoRepository solicitudRepository,
                                  UserRepository userRepository) {
        this.solicitudRepository = solicitudRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public SolicitudAcceso execute(String nombre, String correo, String rol) {
        userRepository.findByEmail(correo).ifPresent(u -> {
            throw new UserAlreadyExistsException(correo);
        });

        solicitudRepository.findByCorreo(correo).ifPresent(existing -> {
            if (existing.getEstado() == EstadoSolicitud.PENDIENTE) {
                throw new SolicitudPendienteException(correo);
            }
            if (existing.getEstado() == EstadoSolicitud.RECHAZADO) {
                LocalDateTime cooldownExpiry = existing.getRejectedAt().plusDays(cooldownDays);
                if (LocalDateTime.now().isBefore(cooldownExpiry)) {
                    long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(
                            LocalDateTime.now(), cooldownExpiry) + 1;
                    throw new SolicitudRechazadaRecientementeException((int) diasRestantes);
                }
                solicitudRepository.delete(existing);
            }
        });

        SolicitudAcceso solicitud = SolicitudAcceso.create(nombre, correo, rol);
        return solicitudRepository.save(solicitud);
    }
}
