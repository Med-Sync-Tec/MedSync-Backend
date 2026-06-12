package itesm.medsync.application.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.pacientecontexto.repository.PacienteContextoRepository;
import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import itesm.medsync.domain.shared.model.TipoClinico;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddPacienteContextoServiceTest {

    @Mock
    PacienteContextoRepository repository;

    @Mock
    PatientRepository patientRepository;

    @InjectMocks
    AddPacienteContextoService service;

    private UUID pacienteId;

    @BeforeEach
    void setUp() {
        pacienteId = UUID.randomUUID();
    }

    private Patient stubPatient() {
        return new Patient(pacienteId, "EXP", "Ana", LocalDate.of(1990, Month.JANUARY, 1),
                "F", UUID.randomUUID(), true, null, null);
    }

    @Test
    @DisplayName("Happy path: guarda y devuelve el contexto")
    void addOk() {
        when(patientRepository.findByUuid(pacienteId)).thenReturn(Optional.of(stubPatient()));
        when(repository.save(any(PacienteContexto.class))).thenAnswer(inv -> inv.getArgument(0));

        PacienteContexto result = service.execute(pacienteId, TipoClinico.ENFERMEDAD, "Hipertensión", null);

        assertNotNull(result);
        assertEquals(pacienteId, result.getPacienteId());
        assertEquals(TipoClinico.ENFERMEDAD, result.getTipo());
        assertEquals("Hipertensión", result.getValor());
        assertNull(result.getEspecialidadId());

        ArgumentCaptor<PacienteContexto> captor = ArgumentCaptor.forClass(PacienteContexto.class);
        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue().getId());
    }

    @Test
    @DisplayName("Con especialidadId del caller: lo propaga al PacienteContexto persistido")
    void addInheritsCallerSpecialty() {
        UUID especialidadId = UUID.randomUUID();
        when(patientRepository.findByUuid(pacienteId)).thenReturn(Optional.of(stubPatient()));
        when(repository.save(any(PacienteContexto.class))).thenAnswer(inv -> inv.getArgument(0));

        PacienteContexto result = service.execute(pacienteId, TipoClinico.MEDICAMENTO,
                "Losartán", especialidadId);

        assertEquals(especialidadId, result.getEspecialidadId());
    }

    @Test
    @DisplayName("Paciente inexistente lanza PatientNotFoundException, no guarda")
    void addPatientNotFound() {
        when(patientRepository.findByUuid(pacienteId)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class,
                () -> service.execute(pacienteId, TipoClinico.SINTOMA, "Mareo", null));

        verify(repository, never()).save(any());
    }
}
