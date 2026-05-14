package itesm.medsync.domain.solicitud.usecase;

import itesm.medsync.domain.solicitud.model.AprobarSolicitudResult;

public interface AprobarSolicitudUseCase {
    AprobarSolicitudResult execute(String token);
}
