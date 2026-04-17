package itesm.medsync.application.patient;

import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPatientByIdServiceTest {

    @Mock
    PatientRepository repository;

    @InjectMocks
    GetPatientByIdService service;

    @Test
    @DisplayName("Id existente devuelve el paciente")
    void getOk() {
        UUID id = UUID.randomUUID();
        Patient p = new Patient(id, "EXP-1", "Juan", LocalDate.of(1990, 1, 1),
                "M", UUID.randomUUID(), true, null, null);
        when(repository.findById(id)).thenReturn(Optional.of(p));

        Patient result = service.execute(id);

        assertEquals(p, result);
    }

    @Test
    @DisplayName("Id inexistente lanza PatientNotFoundException")
    void getNotFound() {
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> service.execute(missing));
    }
}
