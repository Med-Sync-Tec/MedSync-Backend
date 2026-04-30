package itesm.medsync.domain.pacientecontexto.model;

import itesm.medsync.domain.pacientecontexto.exception.InvalidPacienteContextoDataException;
import itesm.medsync.domain.shared.model.TipoClinico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PacienteContextoTest {

    @Test
    @DisplayName("create asigna id y deja createdAt nulo (lo pone Hibernate)")
    void createAssignsId() {
        UUID pacienteId = UUID.randomUUID();
        PacienteContexto ctx = PacienteContexto.create(
                pacienteId, TipoClinico.ENFERMEDAD, "Hipertensión");

        assertNotNull(ctx.getId());
        assertEquals(pacienteId, ctx.getPacienteId());
        assertEquals(TipoClinico.ENFERMEDAD, ctx.getTipo());
        assertEquals("Hipertensión", ctx.getValor());
        assertNull(ctx.getCreatedAt());
    }

    @Test
    @DisplayName("pacienteId null lanza InvalidPacienteContextoDataException")
    void pacienteIdNullThrows() {
        assertThrows(InvalidPacienteContextoDataException.class,
                () -> PacienteContexto.create(null, TipoClinico.ENFERMEDAD, "x"));
    }

    @Test
    @DisplayName("tipo null lanza InvalidPacienteContextoDataException")
    void tipoNullThrows() {
        assertThrows(InvalidPacienteContextoDataException.class,
                () -> PacienteContexto.create(UUID.randomUUID(), null, "x"));
    }

    @Test
    @DisplayName("valor blank lanza InvalidPacienteContextoDataException")
    void valorBlankThrows() {
        assertThrows(InvalidPacienteContextoDataException.class,
                () -> PacienteContexto.create(UUID.randomUUID(), TipoClinico.ENFERMEDAD, "   "));
    }
}
