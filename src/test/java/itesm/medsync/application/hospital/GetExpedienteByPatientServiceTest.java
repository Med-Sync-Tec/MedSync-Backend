package itesm.medsync.application.hospital;

import itesm.medsync.domain.hospital.exception.ExpedienteNotFoundException;
import itesm.medsync.domain.hospital.model.ExpedienteClinico;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetExpedienteByPatientServiceTest {

    @Mock
    GetPatientByIdUseCase getPatientByIdUseCase;

    @Mock
    HospitalGateway hospitalGateway;

    @InjectMocks
    GetExpedienteByPatientService service;

    private Patient newPatient(UUID id, String expedienteExternoId) {
        return new Patient(id, expedienteExternoId, "Juan", LocalDate.of(1990, 1, 1),
                "M", UUID.randomUUID(), true, null, null);
    }

    @Test
    @DisplayName("Paciente existe + expediente existe → devuelve expediente")
    void happyPath() {
        UUID patientId = UUID.randomUUID();
        Patient p = newPatient(patientId, "EXP-EXT-1");
        ExpedienteClinico exp = new ExpedienteClinico("EXP-HOSP-1", "EXP-EXT-1", "DR-1", null, null);
        when(getPatientByIdUseCase.execute(patientId)).thenReturn(p);
        when(hospitalGateway.findExpedienteByPacienteExternoId("EXP-EXT-1"))
                .thenReturn(Optional.of(exp));

        ExpedienteClinico result = service.execute(patientId);

        assertEquals(exp, result);
    }

    @Test
    @DisplayName("Paciente existe pero sin expediente → ExpedienteNotFoundException")
    void expedienteNotFound() {
        UUID patientId = UUID.randomUUID();
        Patient p = newPatient(patientId, "EXP-EXT-2");
        when(getPatientByIdUseCase.execute(patientId)).thenReturn(p);
        when(hospitalGateway.findExpedienteByPacienteExternoId("EXP-EXT-2"))
                .thenReturn(Optional.empty());

        assertThrows(ExpedienteNotFoundException.class, () -> service.execute(patientId));
    }

    @Test
    @DisplayName("Paciente no existe → propaga PatientNotFoundException, no consulta gateway")
    void patientNotFoundPropagates() {
        UUID missing = UUID.randomUUID();
        when(getPatientByIdUseCase.execute(missing)).thenThrow(new PatientNotFoundException(missing));

        assertThrows(PatientNotFoundException.class, () -> service.execute(missing));
        verify(hospitalGateway, never()).findExpedienteByPacienteExternoId(org.mockito.ArgumentMatchers.anyString());
    }
}
