package itesm.medsync.application.patient;

import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletePatientServiceTest {

    @Mock
    PatientRepository repository;

    @InjectMocks
    DeletePatientService service;

    @Test
    @DisplayName("Paciente existente → soft delete guarda con activo=false")
    void deleteOk() {
        UUID id = UUID.randomUUID();
        Patient active = new Patient(id, "EXP-1", "Juan", LocalDate.of(1990, Month.JANUARY, 1),
                "M", UUID.randomUUID(), true, null, null);
        when(repository.findByUuid(id)).thenReturn(Optional.of(active));
        when(repository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        service.execute(id);

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(repository).save(captor.capture());
        assertFalse(captor.getValue().isActivo());
        assertEquals(id, captor.getValue().getId());
    }

    @Test
    @DisplayName("Paciente inexistente → PatientNotFoundException, no guarda")
    void deleteNotFound() {
        UUID missing = UUID.randomUUID();
        when(repository.findByUuid(missing)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> service.execute(missing));
        verify(repository, never()).save(any());
    }
}
