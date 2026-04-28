package itesm.medsync.application.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.pacientecontexto.repository.PacienteContextoRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListPacienteContextosByPacienteServiceTest {

    @Mock
    PacienteContextoRepository repository;

    @Mock
    PatientRepository patientRepository;

    @InjectMocks
    ListPacienteContextosByPacienteService service;

    @Test
    @DisplayName("Devuelve la lista de contextos del paciente")
    void listOk() {
        UUID pacienteId = UUID.randomUUID();
        when(patientRepository.findByUuid(pacienteId)).thenReturn(
                Optional.of(new Patient(pacienteId, "EXP", "Ana",
                        LocalDate.of(1990, 1, 1), "F", UUID.randomUUID(), true, null, null)));
        PacienteContexto ctx = PacienteContexto.create(pacienteId,
                PacienteContexto.Tipo.ENFERMEDAD, "HTA");
        when(repository.findByPacienteId(pacienteId)).thenReturn(List.of(ctx));

        List<PacienteContexto> result = service.execute(pacienteId);
        assertEquals(1, result.size());
        assertEquals("HTA", result.get(0).getValor());
    }

    @Test
    @DisplayName("Paciente inexistente → PatientNotFoundException")
    void listPatientNotFound() {
        UUID pacienteId = UUID.randomUUID();
        when(patientRepository.findByUuid(pacienteId)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> service.execute(pacienteId));
        verify(repository, never()).findByPacienteId(any());
    }
}
