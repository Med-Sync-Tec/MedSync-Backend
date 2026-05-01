package itesm.medsync.domain.shared.model;

import itesm.medsync.domain.shared.exception.InvalidTipoClinicoException;

public enum TipoClinico {
    ENFERMEDAD,
    SINTOMA,
    TRATAMIENTO,
    MEDICAMENTO;

    public static TipoClinico fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidTipoClinicoException("tipo cannot be null or blank");
        }
        try {
            return TipoClinico.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidTipoClinicoException(
                    "tipo inválido: '" + raw + "'. Permitidos: enfermedad, sintoma, tratamiento, medicamento");
        }
    }
}
