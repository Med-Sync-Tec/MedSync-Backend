package itesm.medsync.domain.solicitud.model;

import itesm.medsync.domain.user.model.UserWithRole;

public record AprobarSolicitudResult(UserWithRole user, String passwordResetLink) {}
