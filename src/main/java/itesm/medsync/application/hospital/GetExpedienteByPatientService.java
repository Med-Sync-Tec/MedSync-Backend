package itesm.medsync.application.hospital;

import itesm.medsync.domain.hospital.exception.ExpedienteNotFoundException;
import itesm.medsync.domain.hospital.model.ExpedienteClinico;
import itesm.medsync.domain.hospital.repository.HospitalGateway;
import itesm.medsync.domain.hospital.usecase.GetExpedienteByPatientUseCase;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.usecase.GetPatientByIdUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class GetExpedienteByPatientService implements GetExpedienteByPatientUseCase {

    private final GetPatientByIdUseCase getPatientByIdUseCase;
    private final HospitalGateway hospitalGateway;

    @Inject
    public GetExpedienteByPatientService(GetPatientByIdUseCase getPatientByIdUseCase,
                                         HospitalGateway hospitalGateway) {
        this.getPatientByIdUseCase = getPatientByIdUseCase;
        this.hospitalGateway = hospitalGateway;
    }

    @Override
    public ExpedienteClinico execute(UUID patientId) {
        Patient patient = getPatientByIdUseCase.execute(patientId);
        return hospitalGateway.findExpedienteByPacienteExternoId(patient.getExpedienteExternoId())
                .orElseThrow(() -> new ExpedienteNotFoundException(patientId));
    }
}
