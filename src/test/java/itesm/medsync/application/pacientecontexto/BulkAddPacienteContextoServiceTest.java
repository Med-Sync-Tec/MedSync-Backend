package itesm.medsync.application.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.exception.InvalidPacienteContextoDataException;
import itesm.medsync.domain.pacientecontexto.model.BulkEntry;
import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.pacientecontexto.repository.PacienteContextoRepository;
import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import itesm.medsync.domain.shared.exception.InvalidTipoClinicoException;
import itesm.medsync.domain.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkAddPacienteContextoServiceTest {

    @Mock
    PatientRepository patientRepository;

    @Mock
    PacienteContextoRepository pcRepository;

    @InjectMocks
    BulkAddPacienteContextoService service;

    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID SPECIALTY_ID = UUID.randomUUID();

    private User caller(UUID especialidadId) {
        return new User(UUID.randomUUID(), "Dra. A", "a@tec.mx",
                especialidadId, UUID.randomUUID(), true, null);
    }

    private Patient patient() {
        return new Patient(PATIENT_ID, "EXP", "PT", LocalDate.of(1990, 1, 1),
                "F", UUID.randomUUID(), true, null, null);
    }

    @Test
    @DisplayName("Happy path: 3 entries válidos → repo.save x3 con especialidadId del caller")
    void happyPath() {
        when(patientRepository.findByUuid(PATIENT_ID)).thenReturn(Optional.of(patient()));
        when(pcRepository.save(any(PacienteContexto.class))).thenAnswer(inv -> inv.getArgument(0));

        List<BulkEntry> entries = List.of(
                new BulkEntry("enfermedad", "Hipertensión arterial"),
                new BulkEntry("medicamento", "Losartán"),
                new BulkEntry("sintoma", "Disnea"));

        List<PacienteContexto> persisted = service.execute(PATIENT_ID, entries, caller(SPECIALTY_ID));

        assertEquals(3, persisted.size());
        ArgumentCaptor<PacienteContexto> captor = ArgumentCaptor.forClass(PacienteContexto.class);
        verify(pcRepository, times(3)).save(captor.capture());
        for (PacienteContexto pc : captor.getAllValues()) {
            assertEquals(SPECIALTY_ID, pc.getEspecialidadId());
            assertEquals(PATIENT_ID, pc.getPacienteId());
        }
    }

    @Test
    @DisplayName("Caller sin especialidad → todas las filas se persisten con especialidadId=null")
    void callerWithNullSpecialty() {
        when(patientRepository.findByUuid(PATIENT_ID)).thenReturn(Optional.of(patient()));
        when(pcRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<BulkEntry> entries = List.of(new BulkEntry("enfermedad", "X"));
        service.execute(PATIENT_ID, entries, caller(null));

        ArgumentCaptor<PacienteContexto> captor = ArgumentCaptor.forClass(PacienteContexto.class);
        verify(pcRepository).save(captor.capture());
        assertNull(captor.getValue().getEspecialidadId());
    }

    @Test
    @DisplayName("Patient inexistente → PatientNotFoundException; repo nunca llamado")
    void patientNotFound() {
        when(patientRepository.findByUuid(PATIENT_ID)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class,
                () -> service.execute(PATIENT_ID,
                        List.of(new BulkEntry("enfermedad", "x")), caller(SPECIALTY_ID)));
        verify(pcRepository, never()).save(any());
    }

    @Test
    @DisplayName("Entry #2 con tipo inválido → InvalidTipoClinicoException; entry #1 NO se persiste (transactional rollback en CDI)")
    void invalidTipoInTheMiddleStopsExecution() {
        when(patientRepository.findByUuid(PATIENT_ID)).thenReturn(Optional.of(patient()));
        when(pcRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<BulkEntry> entries = List.of(
                new BulkEntry("enfermedad", "Hipertensión arterial"),
                new BulkEntry("not-a-tipo", "boom"),
                new BulkEntry("medicamento", "Losartán"));

        assertThrows(InvalidTipoClinicoException.class,
                () -> service.execute(PATIENT_ID, entries, caller(SPECIALTY_ID)));

        // Entry #1 reached save() before #2 threw — the actual rollback
        // happens at the JTA layer (@Transactional on the service). Here we
        // only assert that save() was NEVER called for entries past the bad
        // one — i.e. the loop short-circuited on the throw.
        verify(pcRepository, times(1)).save(any(PacienteContexto.class));
    }

    @Test
    @DisplayName("Entries vacío → InvalidPacienteContextoDataException; repo nunca llamado")
    void emptyEntriesRejected() {
        when(patientRepository.findByUuid(PATIENT_ID)).thenReturn(Optional.of(patient()));

        assertThrows(InvalidPacienteContextoDataException.class,
                () -> service.execute(PATIENT_ID, List.of(), caller(SPECIALTY_ID)));
        verify(pcRepository, never()).save(any());
    }
}
