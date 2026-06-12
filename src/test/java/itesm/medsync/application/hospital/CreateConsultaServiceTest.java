package itesm.medsync.application.hospital;

import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.model.ExpedienteClinico;
import itesm.medsync.domain.hospital.repository.HospitalGateway;
import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.usecase.GetPatientByIdUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateConsultaServiceTest {

    @Mock
    GetPatientByIdUseCase getPatientByIdUseCase;

    @Mock
    HospitalGateway hospitalGateway;

    @InjectMocks
    CreateConsultaService service;

    private Patient newPatient(UUID id, String pacExt) {
        return new Patient(id, pacExt, "Juan", LocalDate.of(1990, Month.JANUARY, 1),
                "M", UUID.randomUUID(), true, null, null);
    }

    @Test
    @DisplayName("Happy path: paciente con expediente → crea consulta usando expediente existente")
    void createWithExistingExpediente() {
        UUID patientId = UUID.randomUUID();
        String pacExt = "PAC-EXT-A";
        Patient p = newPatient(patientId, pacExt);
        ExpedienteClinico existing = new ExpedienteClinico("EXP-EXISTING", pacExt, "DR-1", null, null);
        when(getPatientByIdUseCase.execute(patientId)).thenReturn(p);
        when(hospitalGateway.findExpedienteByPacienteExternoId(pacExt)).thenReturn(Optional.of(existing));
        when(hospitalGateway.saveConsulta(any(Consulta.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime fecha = LocalDateTime.of(2026, 4, 20, 10, 0);
        Consulta result = service.execute(patientId, fecha, "cefalea",
                "refiere dolor", "TA 120/80", "cefalea tensional",
                "reposo", "paracetamol", "cefalea tensional");

        verify(hospitalGateway, never()).saveExpediente(any());
        ArgumentCaptor<Consulta> captor = ArgumentCaptor.forClass(Consulta.class);
        verify(hospitalGateway).saveConsulta(captor.capture());
        Consulta saved = captor.getValue();
        assertEquals("EXP-EXISTING", saved.getExpedienteId());
        assertEquals(fecha, saved.getFecha());
        assertEquals("cefalea", saved.getMotivoConsulta());
        assertEquals("paracetamol", saved.getPrescripcion());
        assertNotNull(saved.getId());
        assertEquals(saved, result);
    }

    @Test
    @DisplayName("Paciente sin expediente → auto-crea expediente + consulta con ese expedienteId")
    void createWithAutoCreatedExpediente() {
        UUID patientId = UUID.randomUUID();
        String pacExt = "PAC-EXT-NEW";
        Patient p = newPatient(patientId, pacExt);
        when(getPatientByIdUseCase.execute(patientId)).thenReturn(p);
        when(hospitalGateway.findExpedienteByPacienteExternoId(pacExt)).thenReturn(Optional.empty());
        when(hospitalGateway.saveExpediente(any(ExpedienteClinico.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(hospitalGateway.saveConsulta(any(Consulta.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime fecha = LocalDateTime.of(2026, 4, 20, 11, 0);
        Consulta result = service.execute(patientId, fecha, "chequeo",
                null, null, null, null, null, null);

        ArgumentCaptor<ExpedienteClinico> expCaptor = ArgumentCaptor.forClass(ExpedienteClinico.class);
        verify(hospitalGateway).saveExpediente(expCaptor.capture());
        ExpedienteClinico savedExp = expCaptor.getValue();
        assertEquals(pacExt, savedExp.getPacienteExternoId());
        assertNotNull(savedExp.getId());

        ArgumentCaptor<Consulta> consCaptor = ArgumentCaptor.forClass(Consulta.class);
        verify(hospitalGateway).saveConsulta(consCaptor.capture());
        Consulta savedCons = consCaptor.getValue();
        assertEquals(savedExp.getId(), savedCons.getExpedienteId(),
                "La consulta debe apuntar al expediente recién creado");
        assertEquals(result, savedCons);
    }

    @Test
    @DisplayName("Paciente no existe → propaga PatientNotFoundException, no toca gateway")
    void patientNotFoundPropagates() {
        UUID missing = UUID.randomUUID();
        when(getPatientByIdUseCase.execute(missing)).thenThrow(new PatientNotFoundException(missing));

        assertThrows(PatientNotFoundException.class,
                () -> service.execute(missing, LocalDateTime.now(),
                        null, null, null, null, null, null, null));

        verify(hospitalGateway, never()).findExpedienteByPacienteExternoId(anyString());
        verify(hospitalGateway, never()).saveExpediente(any());
        verify(hospitalGateway, never()).saveConsulta(any());
    }
}
