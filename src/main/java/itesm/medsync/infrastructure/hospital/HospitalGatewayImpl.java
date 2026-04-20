package itesm.medsync.infrastructure.hospital;

import io.quarkus.hibernate.orm.PersistenceUnit;
import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.model.ExpedienteClinico;
import itesm.medsync.domain.hospital.repository.HospitalGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class HospitalGatewayImpl implements HospitalGateway {

    private final EntityManager em;

    @Inject
    public HospitalGatewayImpl(@PersistenceUnit("hospital") EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<ExpedienteClinico> findExpedienteByPacienteExternoId(String pacienteExternoId) {
        return em.createQuery(
                        "SELECT e FROM ExpedienteClinicoHospitalEntity e " +
                                "WHERE e.pacienteExternoId = :pacExt",
                        ExpedienteClinicoHospitalEntity.class)
                .setParameter("pacExt", pacienteExternoId)
                .getResultStream()
                .findFirst()
                .map(HospitalPersistenceMapper::toDomain);
    }

    @Override
    public List<Consulta> findConsultasByPacienteExternoId(String pacienteExternoId) {
        return em.createQuery(
                        "SELECT c FROM ConsultaHospitalEntity c " +
                                "WHERE c.expedienteId IN (" +
                                "    SELECT e.id FROM ExpedienteClinicoHospitalEntity e " +
                                "    WHERE e.pacienteExternoId = :pacExt" +
                                ") ORDER BY c.fecha DESC",
                        ConsultaHospitalEntity.class)
                .setParameter("pacExt", pacienteExternoId)
                .getResultList()
                .stream()
                .map(HospitalPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public ExpedienteClinico saveExpediente(ExpedienteClinico expediente) {
        ExpedienteClinicoHospitalEntity entity = HospitalPersistenceMapper.toEntity(expediente);
        em.persist(entity);
        em.flush();
        return HospitalPersistenceMapper.toDomain(entity);
    }

    @Override
    public Consulta saveConsulta(Consulta consulta) {
        ConsultaHospitalEntity entity = HospitalPersistenceMapper.toEntity(consulta);
        em.persist(entity);
        em.flush();
        return HospitalPersistenceMapper.toDomain(entity);
    }

    @Override
    public Optional<Consulta> findConsultaById(String consultaId) {
        return em.createQuery(
                        "SELECT c FROM ConsultaHospitalEntity c WHERE c.id = :id",
                        ConsultaHospitalEntity.class)
                .setParameter("id", consultaId)
                .getResultStream()
                .findFirst()
                .map(HospitalPersistenceMapper::toDomain);
    }
}
