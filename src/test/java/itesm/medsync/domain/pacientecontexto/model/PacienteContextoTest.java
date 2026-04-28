package itesm.medsync.domain.pacientecontexto.model;

import itesm.medsync.domain.pacientecontexto.exception.InvalidPacienteContextoDataException;
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
                pacienteId,
                PacienteContexto.Tipo.ENFERMEDAD,
                "Hipertensión");

        assertNotNull(ctx.getId());
        assertEquals(pacienteId, ctx.getPacienteId());
        assertEquals(PacienteContexto.Tipo.ENFERMEDAD, ctx.getTipo());
        assertEquals("Hipertensión", ctx.getValor());
        assertNull(ctx.getCreatedAt());
    }

    @Test
    @DisplayName("pacienteId null lanza InvalidPacienteContextoDataException")
    void pacienteIdNullThrows() {
        assertThrows(InvalidPacienteContextoDataException.class,
                () -> PacienteContexto.create(
                        null,
                        PacienteContexto.Tipo.ENFERMEDAD,
                        "x"));
    }

    @Test
    @DisplayName("tipo null lanza InvalidPacienteContextoDataException")
    void tipoNullThrows() {
        assertThrows(InvalidPacienteContextoDataException.class,
                () -> PacienteContexto.create(
                        UUID.randomUUID(),
                        null,
                        "x"));
    }

    @Test
    @DisplayName("valor blank lanza InvalidPacienteContextoDataException")
    void valorBlankThrows() {
        assertThrows(InvalidPacienteContextoDataException.class,
                () -> PacienteContexto.create(
                        UUID.randomUUID(),
                        PacienteContexto.Tipo.ENFERMEDAD,
                        "   "));
    }

    @Test
    @DisplayName("Tipo.fromString case-insensitive y reconoce variantes")
    void tipoFromString() {
        assertEquals(PacienteContexto.Tipo.ENFERMEDAD, PacienteContexto.Tipo.fromString("enfermedad"));
        assertEquals(PacienteContexto.Tipo.MEDICAMENTO, PacienteContexto.Tipo.fromString("MEDICAMENTO"));
        assertThrows(InvalidPacienteContextoDataException.class,
                () -> PacienteContexto.Tipo.fromString("otro"));
        assertThrows(InvalidPacienteContextoDataException.class,
                () -> PacienteContexto.Tipo.fromString(null));
    }
}
