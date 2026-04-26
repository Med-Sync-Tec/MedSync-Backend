package itesm.medsync.application.hospital;

import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.model.ExpedienteClinico;
import itesm.medsync.domain.hospital.repository.HospitalGateway;
import itesm.medsync.domain.hospital.usecase.CreateConsultaUseCase;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.usecase.GetPatientByIdUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class CreateConsultaService implements CreateConsultaUseCase {

    private final GetPatientByIdUseCase getPatientByIdUseCase;
    private final HospitalGateway hospitalGateway;

    @Inject
    public CreateConsultaService(GetPatientByIdUseCase getPatientByIdUseCase,
                                 HospitalGateway hospitalGateway) {
        this.getPatientByIdUseCase = getPatientByIdUseCase;
        this.hospitalGateway = hospitalGateway;
    }

    @Override
    public Consulta execute(UUID patientId,
                            LocalDateTime fecha,
                            String motivoConsulta,
                            String subjetivo,
                            String objetivo,
                            String evaluacion,
                            String plan,
                            String prescripcion,
                            String diagnostico) {
        Patient patient = getPatientByIdUseCase.execute(patientId);
        String pacienteExternoId = patient.getExpedienteExternoId();

        ExpedienteClinico expediente = hospitalGateway
                .findExpedienteByPacienteExternoId(pacienteExternoId)
                .orElseGet(() -> hospitalGateway.saveExpediente(
                        ExpedienteClinico.create(pacienteExternoId, null)));

        Consulta consulta = Consulta.create(
                expediente.getId(),
                fecha,
                motivoConsulta,
                subjetivo,
                objetivo,
                evaluacion,
                plan,
                prescripcion,
                diagnostico);

        return hospitalGateway.saveConsulta(consulta);
    }
}
