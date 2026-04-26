package itesm.medsync.application.hospital;

import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.repository.HospitalGateway;
import itesm.medsync.domain.hospital.usecase.GetConsultasByPatientUseCase;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.usecase.GetPatientByIdUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GetConsultasByPatientService implements GetConsultasByPatientUseCase {

    private final GetPatientByIdUseCase getPatientByIdUseCase;
    private final HospitalGateway hospitalGateway;

    @Inject
    public GetConsultasByPatientService(GetPatientByIdUseCase getPatientByIdUseCase,
                                        HospitalGateway hospitalGateway) {
        this.getPatientByIdUseCase = getPatientByIdUseCase;
        this.hospitalGateway = hospitalGateway;
    }

    @Override
    public List<Consulta> execute(UUID patientId) {
        Patient patient = getPatientByIdUseCase.execute(patientId);
        return hospitalGateway.findConsultasByPacienteExternoId(patient.getExpedienteExternoId());
    }
}
