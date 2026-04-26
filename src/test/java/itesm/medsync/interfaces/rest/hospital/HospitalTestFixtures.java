package itesm.medsync.interfaces.rest.hospital;

import io.quarkus.hibernate.orm.PersistenceUnit;
import itesm.medsync.infrastructure.hospital.ConsultaHospitalEntity;
import itesm.medsync.infrastructure.hospital.ExpedienteClinicoHospitalEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@ApplicationScoped
public class HospitalTestFixtures {

    private final EntityManager em;

    @Inject
    public HospitalTestFixtures(@PersistenceUnit("hospital") EntityManager em) {
        this.em = em;
    }

    @Transactional
    public void persistExpediente(String id, String pacienteExternoId, String doctorId) {
        ExpedienteClinicoHospitalEntity e = new ExpedienteClinicoHospitalEntity();
        e.setId(id);
        e.setPacienteExternoId(pacienteExternoId);
        e.setDoctorResponsableId(doctorId);
        em.persist(e);
    }

    @Transactional
    public void persistConsulta(String id, String expedienteId, LocalDateTime fecha,
                                String motivo, String diagnostico) {
        ConsultaHospitalEntity c = new ConsultaHospitalEntity();
        c.setId(id);
        c.setExpedienteId(expedienteId);
        c.setFecha(fecha);
        c.setMotivoConsulta(motivo);
        c.setSubjetivo("subj-" + id);
        c.setObjetivo("obj-" + id);
        c.setEvaluacion("eval-" + id);
        c.setPlan("plan-" + id);
        c.setPrescripcion("presc-" + id);
        c.setDiagnostico(diagnostico);
        em.persist(c);
    }
}
