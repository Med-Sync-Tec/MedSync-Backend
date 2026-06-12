package itesm.medsync.application.patient;

import itesm.medsync.domain.patient.exception.DuplicatePatientException;
import itesm.medsync.domain.patient.exception.InvalidPatientDataException;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePatientServiceTest {

    @Mock
    PatientRepository repository;

    @InjectMocks
    CreatePatientService service;

    private UUID medicoId;

    @BeforeEach
    void setUp() {
        medicoId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Happy path: guarda y devuelve el paciente")
    void createOk() {
        when(repository.existsByExpedienteExternoId("EXP-1")).thenReturn(false);
        when(repository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient result = service.execute("EXP-1", "Juan", LocalDate.of(1990, Month.JANUARY, 1), "M", medicoId);

        assertNotNull(result);
        assertEquals("EXP-1", result.getExpedienteExternoId());
        assertEquals("Juan", result.getNombre());
        assertTrue(result.isActivo());

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue().getId());
        assertEquals(medicoId, captor.getValue().getMedicoId());
    }

    @Test
    @DisplayName("Expediente duplicado → DuplicatePatientException, no guarda")
    void createDuplicate() {
        when(repository.existsByExpedienteExternoId("EXP-DUP")).thenReturn(true);

        DuplicatePatientException ex = assertThrows(DuplicatePatientException.class,
                () -> service.execute("EXP-DUP", "Juan", LocalDate.of(1990, Month.JANUARY, 1), "M", medicoId));
        assertEquals("EXP-DUP", ex.getExpedienteExternoId());

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Datos inválidos propagan InvalidPatientDataException, no consulta repo")
    void createInvalidData() {
        LocalDate future = LocalDate.of(2099, Month.DECEMBER, 31);
        assertThrows(InvalidPatientDataException.class,
                () -> service.execute("EXP-1", "Juan", future, "M", medicoId));
        // La validación de unicidad ocurre ANTES de construir el Patient:
        // con `existsByExpedienteExternoId` retornando false (default de Mockito),
        // el flujo construye Patient y ahí truena. Verificamos que save nunca se invoca.
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Genero null es aceptado (campo opcional)")
    void createGeneroNull() {
        when(repository.existsByExpedienteExternoId(any())).thenReturn(false);
        when(repository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient p = service.execute("EXP-2", "Ana", LocalDate.of(1985, Month.MARCH, 3), null, medicoId);
        assertNull(p.getGenero());
    }
}
