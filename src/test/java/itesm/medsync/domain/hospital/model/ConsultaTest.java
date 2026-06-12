package itesm.medsync.domain.hospital.model;

import itesm.medsync.domain.hospital.exception.InvalidHospitalDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ConsultaTest {

    private static final String VALID_ID = "CONS-001";
    private static final String VALID_EXP_ID = "EXP-HOSP-001";
    private static final LocalDateTime VALID_FECHA = LocalDateTime.of(2026, 1, 15, 10, 30);

    private Consulta newValid() {
        return new Consulta(VALID_ID, VALID_EXP_ID, VALID_FECHA,
                "Dolor de cabeza", "Paciente refiere cefalea", "TA 120/80",
                "Cefalea tensional", "Analgésico c/8h", "Paracetamol 500mg",
                "Cefalea tensional", null, null);
    }

    @Test
    @DisplayName("Constructor válido crea instancia con todos los campos correctos")
    void constructorValid() {
        Consulta c = newValid();
        assertEquals(VALID_ID, c.getId());
        assertEquals(VALID_EXP_ID, c.getExpedienteId());
        assertEquals(VALID_FECHA, c.getFecha());
        assertEquals("Dolor de cabeza", c.getMotivoConsulta());
        assertEquals("Paciente refiere cefalea", c.getSubjetivo());
        assertEquals("TA 120/80", c.getObjetivo());
        assertEquals("Cefalea tensional", c.getEvaluacion());
        assertEquals("Analgésico c/8h", c.getPlan());
        assertEquals("Paracetamol 500mg", c.getPrescripcion());
        assertEquals("Cefalea tensional", c.getDiagnostico());
    }

    @Test
    @DisplayName("Campos SOAP opcionales pueden ser null")
    void constructorSoapNullable() {
        Consulta c = new Consulta(VALID_ID, VALID_EXP_ID, VALID_FECHA,
                null, null, null, null, null, null, null, null, null);
        assertNull(c.getMotivoConsulta());
        assertNull(c.getSubjetivo());
    }

    @ParameterizedTest(name = "id inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("id null/blank lanza InvalidHospitalDataException")
    void constructorIdBlank(String invalid) {
        assertThrows(InvalidHospitalDataException.class,
                () -> new Consulta(invalid, VALID_EXP_ID, VALID_FECHA,
                        null, null, null, null, null, null, null, null, null));
    }

    @ParameterizedTest(name = "expedienteId inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("expedienteId null/blank lanza InvalidHospitalDataException")
    void constructorExpIdBlank(String invalid) {
        assertThrows(InvalidHospitalDataException.class,
                () -> new Consulta(VALID_ID, invalid, VALID_FECHA,
                        null, null, null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("fecha null lanza InvalidHospitalDataException")
    void constructorFechaNull() {
        assertThrows(InvalidHospitalDataException.class,
                () -> new Consulta(VALID_ID, VALID_EXP_ID, null,
                        null, null, null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("Consulta.create genera id, sin timestamps, copia campos SOAP")
    void factoryCreate() {
        Consulta c = Consulta.create("EXP-1", VALID_FECHA,
                "motivo", "subj", "obj", "eval", "plan", "presc", "diag");

        assertNotNull(c.getId());
        assertFalse(c.getId().isBlank());
        assertEquals("EXP-1", c.getExpedienteId());
        assertEquals(VALID_FECHA, c.getFecha());
        assertEquals("motivo", c.getMotivoConsulta());
        assertEquals("subj", c.getSubjetivo());
        assertEquals("obj", c.getObjetivo());
        assertEquals("eval", c.getEvaluacion());
        assertEquals("plan", c.getPlan());
        assertEquals("presc", c.getPrescripcion());
        assertEquals("diag", c.getDiagnostico());
        assertNull(c.getCreatedAt());
        assertNull(c.getUpdatedAt());
    }

    @Test
    @DisplayName("Consulta.create con campos SOAP opcionales en null es válido")
    void factoryCreateNullFields() {
        Consulta c = Consulta.create("EXP-1", VALID_FECHA,
                null, null, null, null, null, null, null);
        assertNotNull(c.getId());
        assertNull(c.getMotivoConsulta());
    }

    @Test
    @DisplayName("Consulta.create con expedienteId blank lanza excepción")
    void factoryCreateBlankExpediente() {
        assertThrows(InvalidHospitalDataException.class,
                () -> Consulta.create("  ", VALID_FECHA,
                        null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("Consulta.create con fecha null lanza excepción")
    void factoryCreateNullFecha() {
        assertThrows(InvalidHospitalDataException.class,
                () -> Consulta.create("EXP-1", null,
                        null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("equals/hashCode basados en id")
    void equalsAndHashCode() {
        Consulta a = new Consulta(VALID_ID, VALID_EXP_ID, VALID_FECHA,
                null, null, null, null, null, null, null, null, null);
        Consulta b = new Consulta(VALID_ID, "OTRO-EXP", LocalDateTime.of(2025, 1, 15, 10, 0),
                "otro motivo", null, null, null, null, null, null, null, null);
        Consulta c = new Consulta("OTRO-ID", VALID_EXP_ID, VALID_FECHA,
                null, null, null, null, null, null, null, null, null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
