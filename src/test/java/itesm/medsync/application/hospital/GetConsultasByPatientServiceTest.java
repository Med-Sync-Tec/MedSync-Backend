package itesm.medsync.application.hospital;

import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.repository.HospitalGateway;
import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.usecase.GetPatientByIdUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetConsultasByPatientServiceTest {

    @Mock
    GetPatientByIdUseCase getPatientByIdUseCase;

    @Mock
    HospitalGateway hospitalGateway;

    @InjectMocks
    GetConsultasByPatientService service;

    private Patient newPatient(UUID id, String expedienteExternoId) {
        return new Patient(id, expedienteExternoId, "Juan", LocalDate.of(1990, 1, 1),
                "M", UUID.randomUUID(), true, null, null);
    }

    private Consulta newConsulta(String id) {
        return new Consulta(id, "EXP-HOSP-1", LocalDateTime.now(),
                "motivo", null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("Paciente existe con consultas → devuelve lista no vacía")
    void returnsConsultas() {
        UUID patientId = UUID.randomUUID();
        Patient p = newPatient(patientId, "EXP-EXT-1");
        List<Consulta> consultas = List.of(newConsulta("C-1"), newConsulta("C-2"));
        when(getPatientByIdUseCase.execute(patientId)).thenReturn(p);
        when(hospitalGateway.findConsultasByPacienteExternoId("EXP-EXT-1")).thenReturn(consultas);

        List<Consulta> result = service.execute(patientId);

        assertEquals(2, result.size());
        assertEquals(consultas, result);
    }

    @Test
    @DisplayName("Paciente existe sin consultas → lista vacía, no lanza excepción")
    void noConsultasReturnsEmpty() {
        UUID patientId = UUID.randomUUID();
        Patient p = newPatient(patientId, "EXP-EXT-2");
        when(getPatientByIdUseCase.execute(patientId)).thenReturn(p);
        when(hospitalGateway.findConsultasByPacienteExternoId("EXP-EXT-2")).thenReturn(List.of());

        List<Consulta> result = service.execute(patientId);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Paciente no existe → propaga PatientNotFoundException, no consulta gateway")
    void patientNotFoundPropagates() {
        UUID missing = UUID.randomUUID();
        when(getPatientByIdUseCase.execute(missing)).thenThrow(new PatientNotFoundException(missing));

        assertThrows(PatientNotFoundException.class, () -> service.execute(missing));
        verify(hospitalGateway, never()).findConsultasByPacienteExternoId(org.mockito.ArgumentMatchers.anyString());
    }
}
