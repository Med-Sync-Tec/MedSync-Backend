package itesm.medsync.application.patient;

import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListActivePatientsServiceTest {

    @Mock
    PatientRepository repository;

    @InjectMocks
    ListActivePatientsService service;

    @Test
    @DisplayName("Delega a repository.findAllActive() y devuelve la lista")
    void listActive() {
        Patient p = Patient.create("EXP-1", "Juan", LocalDate.of(1990, 1, 1), "M", UUID.randomUUID());
        when(repository.findAllActive()).thenReturn(List.of(p));

        List<Patient> result = service.execute();

        assertEquals(1, result.size());
        assertEquals(p, result.get(0));
    }

    @Test
    @DisplayName("Lista vacía cuando no hay pacientes activos")
    void listEmpty() {
        when(repository.findAllActive()).thenReturn(List.of());
        assertTrue(service.execute().isEmpty());
    }
}
