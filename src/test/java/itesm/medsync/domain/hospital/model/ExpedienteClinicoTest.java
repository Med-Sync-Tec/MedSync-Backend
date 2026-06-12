package itesm.medsync.domain.hospital.model;

import itesm.medsync.domain.hospital.exception.InvalidHospitalDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

class ExpedienteClinicoTest {

    private static final String VALID_ID = "EXP-HOSP-001";
    private static final String VALID_PAC_EXT = "PAC-EXT-001";
    private static final String VALID_DOCTOR = "DR-001";

    private ExpedienteClinico newValid() {
        return new ExpedienteClinico(VALID_ID, VALID_PAC_EXT, VALID_DOCTOR, null, null);
    }

    @Test
    @DisplayName("Constructor válido crea instancia con todos los campos correctos")
    void constructorValid() {
        ExpedienteClinico e = newValid();
        assertEquals(VALID_ID, e.getId());
        assertEquals(VALID_PAC_EXT, e.getPacienteExternoId());
        assertEquals(VALID_DOCTOR, e.getDoctorResponsableId());
    }

    @Test
    @DisplayName("doctorResponsableId null es válido (opcional)")
    void constructorDoctorNullOk() {
        ExpedienteClinico e = new ExpedienteClinico(VALID_ID, VALID_PAC_EXT, null, null, null);
        assertNull(e.getDoctorResponsableId());
    }

    @ParameterizedTest(name = "id inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("id null/blank lanza InvalidHospitalDataException")
    void constructorIdBlank(String invalid) {
        assertThrows(InvalidHospitalDataException.class,
                () -> new ExpedienteClinico(invalid, VALID_PAC_EXT, VALID_DOCTOR, null, null));
    }

    @ParameterizedTest(name = "pacienteExternoId inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("pacienteExternoId null/blank lanza InvalidHospitalDataException")
    void constructorPacExtBlank(String invalid) {
        assertThrows(InvalidHospitalDataException.class,
                () -> new ExpedienteClinico(VALID_ID, invalid, VALID_DOCTOR, null, null));
    }

    @Test
    @DisplayName("ExpedienteClinico.create genera id, sin timestamps")
    void factoryCreate() {
        ExpedienteClinico e = ExpedienteClinico.create("PAC-EXT-NEW", "DR-1");

        assertNotNull(e.getId());
        assertFalse(e.getId().isBlank());
        assertEquals("PAC-EXT-NEW", e.getPacienteExternoId());
        assertEquals("DR-1", e.getDoctorResponsableId());
        assertNull(e.getCreatedAt());
        assertNull(e.getUpdatedAt());
    }

    @Test
    @DisplayName("ExpedienteClinico.create con doctor null es válido")
    void factoryCreateNullDoctor() {
        ExpedienteClinico e = ExpedienteClinico.create("PAC-EXT-NEW", null);
        assertNull(e.getDoctorResponsableId());
    }

    @Test
    @DisplayName("ExpedienteClinico.create con pacienteExternoId blank lanza excepción")
    void factoryCreateBlankPacExt() {
        assertThrows(InvalidHospitalDataException.class,
                () -> ExpedienteClinico.create("  ", "DR-1"));
    }

    @Test
    @DisplayName("equals/hashCode basados en id")
    void equalsAndHashCode() {
        ExpedienteClinico a = new ExpedienteClinico(VALID_ID, VALID_PAC_EXT, VALID_DOCTOR, null, null);
        ExpedienteClinico b = new ExpedienteClinico(VALID_ID, "OTRO-PAC", "OTRO-DR",
                LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0), LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0));
        ExpedienteClinico c = new ExpedienteClinico("OTRO-ID", VALID_PAC_EXT, VALID_DOCTOR, null, null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
