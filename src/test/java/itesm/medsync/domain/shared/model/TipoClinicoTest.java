package itesm.medsync.domain.shared.model;

import itesm.medsync.domain.shared.exception.InvalidTipoClinicoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TipoClinicoTest {

    @Test
    @DisplayName("fromString case-insensitive y reconoce variantes")
    void fromStringHappyPath() {
        assertEquals(TipoClinico.ENFERMEDAD, TipoClinico.fromString("enfermedad"));
        assertEquals(TipoClinico.MEDICAMENTO, TipoClinico.fromString("MEDICAMENTO"));
        assertEquals(TipoClinico.SINTOMA, TipoClinico.fromString("  sintoma  "));
        assertEquals(TipoClinico.TRATAMIENTO, TipoClinico.fromString("Tratamiento"));
    }

    @Test
    @DisplayName("fromString con valor inválido lanza InvalidTipoClinicoException")
    void fromStringInvalid() {
        assertThrows(InvalidTipoClinicoException.class,
                () -> TipoClinico.fromString("otro"));
    }

    @Test
    @DisplayName("fromString con null o blanco lanza InvalidTipoClinicoException")
    void fromStringNullBlank() {
        assertThrows(InvalidTipoClinicoException.class,
                () -> TipoClinico.fromString(null));
        assertThrows(InvalidTipoClinicoException.class,
                () -> TipoClinico.fromString("   "));
    }
}
