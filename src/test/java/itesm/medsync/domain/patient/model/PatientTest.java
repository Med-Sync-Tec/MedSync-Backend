package itesm.medsync.domain.patient.model;

import itesm.medsync.domain.patient.exception.InvalidPatientDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PatientTest {

    private static final UUID VALID_ID = UUID.randomUUID();
    private static final UUID VALID_MEDICO_ID = UUID.randomUUID();
    private static final String VALID_EXPEDIENTE = "EXP-001";
    private static final String VALID_NOMBRE = "Juan Perez";
    private static final LocalDate VALID_FECHA_NAC = LocalDate.of(1990, 5, 20);

    private Patient newValidPatient() {
        return new Patient(
                VALID_ID, VALID_EXPEDIENTE, VALID_NOMBRE, VALID_FECHA_NAC,
                "M", VALID_MEDICO_ID, true, null, null);
    }

    @Test
    @DisplayName("Constructor válido crea instancia con todos los campos correctos")
    void constructorValid() {
        Patient p = newValidPatient();

        assertEquals(VALID_ID, p.getId());
        assertEquals(VALID_EXPEDIENTE, p.getExpedienteExternoId());
        assertEquals(VALID_NOMBRE, p.getNombre());
        assertEquals(VALID_FECHA_NAC, p.getFechaNacimiento());
        assertEquals("M", p.getGenero());
        assertEquals(VALID_MEDICO_ID, p.getMedicoId());
        assertTrue(p.isActivo());
    }

    @Test
    @DisplayName("Genero null es válido (campo opcional)")
    void constructorGeneroNullOk() {
        Patient p = new Patient(VALID_ID, VALID_EXPEDIENTE, VALID_NOMBRE,
                VALID_FECHA_NAC, null, VALID_MEDICO_ID, true, null, null);
        assertNull(p.getGenero());
    }

    @Test
    @DisplayName("id null lanza InvalidPatientDataException")
    void constructorIdNull() {
        assertThrows(InvalidPatientDataException.class, () -> new Patient(
                null, VALID_EXPEDIENTE, VALID_NOMBRE, VALID_FECHA_NAC,
                "M", VALID_MEDICO_ID, true, null, null));
    }

    @ParameterizedTest(name = "nombre inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("nombre null/blank lanza InvalidPatientDataException")
    void constructorNombreBlank(String invalidNombre) {
        assertThrows(InvalidPatientDataException.class, () -> new Patient(
                VALID_ID, VALID_EXPEDIENTE, invalidNombre, VALID_FECHA_NAC,
                "M", VALID_MEDICO_ID, true, null, null));
    }

    @ParameterizedTest(name = "expediente inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("expedienteExternoId null/blank lanza InvalidPatientDataException")
    void constructorExpedienteBlank(String invalidExpediente) {
        assertThrows(InvalidPatientDataException.class, () -> new Patient(
                VALID_ID, invalidExpediente, VALID_NOMBRE, VALID_FECHA_NAC,
                "M", VALID_MEDICO_ID, true, null, null));
    }

    @Test
    @DisplayName("fechaNacimiento null lanza InvalidPatientDataException")
    void constructorFechaNull() {
        assertThrows(InvalidPatientDataException.class, () -> new Patient(
                VALID_ID, VALID_EXPEDIENTE, VALID_NOMBRE, null,
                "M", VALID_MEDICO_ID, true, null, null));
    }

    @Test
    @DisplayName("fechaNacimiento futura lanza InvalidPatientDataException")
    void constructorFechaFutura() {
        LocalDate future = LocalDate.of(2099, 12, 31);
        assertThrows(InvalidPatientDataException.class, () -> new Patient(
                VALID_ID, VALID_EXPEDIENTE, VALID_NOMBRE, future,
                "M", VALID_MEDICO_ID, true, null, null));
    }

    @Test
    @DisplayName("Edad > 150 años lanza InvalidPatientDataException")
    void constructorEdadExcesiva() {
        LocalDate tooOld = LocalDate.of(1800, 1, 1);
        assertThrows(InvalidPatientDataException.class, () -> new Patient(
                VALID_ID, VALID_EXPEDIENTE, VALID_NOMBRE, tooOld,
                "M", VALID_MEDICO_ID, true, null, null));
    }

    @Test
    @DisplayName("Edad exactamente 150 años es válida")
    void constructorEdadLimite() {
        LocalDate edad150 = LocalDate.of(1875, 1, 1);
        assertDoesNotThrow(() -> new Patient(
                VALID_ID, VALID_EXPEDIENTE, VALID_NOMBRE, edad150,
                "M", VALID_MEDICO_ID, true, null, null));
    }

    @Test
    @DisplayName("medicoId null lanza InvalidPatientDataException")
    void constructorMedicoIdNull() {
        assertThrows(InvalidPatientDataException.class, () -> new Patient(
                VALID_ID, VALID_EXPEDIENTE, VALID_NOMBRE, VALID_FECHA_NAC,
                "M", null, true, null, null));
    }

    @Test
    @DisplayName("Patient.create genera UUID nuevo, activo=true, timestamps null")
    void factoryCreate() {
        Patient p = Patient.create(VALID_EXPEDIENTE, VALID_NOMBRE,
                VALID_FECHA_NAC, "F", VALID_MEDICO_ID);

        assertNotNull(p.getId());
        assertEquals(VALID_EXPEDIENTE, p.getExpedienteExternoId());
        assertEquals(VALID_NOMBRE, p.getNombre());
        assertEquals(VALID_FECHA_NAC, p.getFechaNacimiento());
        assertEquals("F", p.getGenero());
        assertEquals(VALID_MEDICO_ID, p.getMedicoId());
        assertTrue(p.isActivo());
        assertNull(p.getCreatedAt());
        assertNull(p.getUpdatedAt());
    }

    @Test
    @DisplayName("Patient.create con genero null es válido")
    void factoryCreateGeneroNull() {
        Patient p = Patient.create(VALID_EXPEDIENTE, VALID_NOMBRE,
                VALID_FECHA_NAC, null, VALID_MEDICO_ID);
        assertNull(p.getGenero());
    }

    @Test
    @DisplayName("softDelete devuelve nueva instancia con activo=false, no muta original")
    void softDeleteImmutable() {
        Patient original = newValidPatient();
        Patient deleted = original.softDelete();

        assertNotSame(original, deleted);
        assertTrue(original.isActivo(), "Original NO debe ser modificado");
        assertFalse(deleted.isActivo(), "Nueva instancia tiene activo=false");
        assertEquals(original.getId(), deleted.getId());
        assertEquals(original.getExpedienteExternoId(), deleted.getExpedienteExternoId());
        assertEquals(original.getNombre(), deleted.getNombre());
    }

    @Test
    @DisplayName("softDelete sobre un patient ya inactivo igual devuelve una instancia inactiva")
    void softDeleteAlreadyInactive() {
        Patient inactive = new Patient(VALID_ID, VALID_EXPEDIENTE, VALID_NOMBRE,
                VALID_FECHA_NAC, "M", VALID_MEDICO_ID, false, null, null);
        Patient result = inactive.softDelete();
        assertFalse(result.isActivo());
    }

    @Test
    @DisplayName("equals/hashCode basados en id")
    void equalsAndHashCode() {
        Patient a = new Patient(VALID_ID, VALID_EXPEDIENTE, VALID_NOMBRE,
                VALID_FECHA_NAC, "M", VALID_MEDICO_ID, true, null, null);
        Patient b = new Patient(VALID_ID, "OTRO-EXP", "Otro Nombre",
                LocalDate.of(1985, 1, 1), "F", UUID.randomUUID(), false,
                LocalDateTime.of(2025, 1, 15, 10, 0), LocalDateTime.of(2025, 1, 15, 10, 0));
        Patient c = new Patient(UUID.randomUUID(), VALID_EXPEDIENTE, VALID_NOMBRE,
                VALID_FECHA_NAC, "M", VALID_MEDICO_ID, true, null, null);

        assertEquals(a, b, "Mismo id → iguales aunque otros campos difieran");
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c, "Distinto id → distintos");
    }
}
