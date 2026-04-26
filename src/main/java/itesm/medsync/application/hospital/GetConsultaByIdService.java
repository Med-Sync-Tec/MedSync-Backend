package itesm.medsync.application.hospital;

import itesm.medsync.domain.hospital.exception.ConsultaNotFoundException;
import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.repository.HospitalGateway;
import itesm.medsync.domain.hospital.usecase.GetConsultaByIdUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetConsultaByIdService implements GetConsultaByIdUseCase {

    private final HospitalGateway hospitalGateway;

    @Inject
    public GetConsultaByIdService(HospitalGateway hospitalGateway) {
        this.hospitalGateway = hospitalGateway;
    }

    @Override
    public Consulta execute(String consultaId) {
        return hospitalGateway.findConsultaById(consultaId)
                .orElseThrow(() -> new ConsultaNotFoundException(consultaId));
    }
}
