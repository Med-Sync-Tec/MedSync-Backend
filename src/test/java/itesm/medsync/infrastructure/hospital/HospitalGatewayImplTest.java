package itesm.medsync.infrastructure.hospital;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.model.ExpedienteClinico;
import itesm.medsync.domain.hospital.repository.HospitalGateway;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class HospitalGatewayImplTest {

    @Inject
    HospitalGateway gateway;

    @Inject
    @PersistenceUnit("hospital")
    EntityManager em;

    private ExpedienteClinicoHospitalEntity seedExpediente(String id, String pacExt) {
        ExpedienteClinicoHospitalEntity e = new ExpedienteClinicoHospitalEntity();
        e.setId(id);
        e.setPacienteExternoId(pacExt);
        e.setDoctorResponsableId("DR-1");
        em.persist(e);
        return e;
    }

    private ConsultaHospitalEntity seedConsulta(String id, String expedienteId, LocalDateTime fecha) {
        ConsultaHospitalEntity c = new ConsultaHospitalEntity();
        c.setId(id);
        c.setExpedienteId(expedienteId);
        c.setFecha(fecha);
        c.setMotivoConsulta("motivo-" + id);
        c.setSubjetivo("subj");
        c.setObjetivo("obj");
        c.setEvaluacion("eval");
        c.setPlan("plan");
        c.setPrescripcion("presc");
        c.setDiagnostico("diag");
        em.persist(c);
        return c;
    }

    @Test
    @TestTransaction
    @DisplayName("findExpedienteByPacienteExternoId: existe → Optional con expediente")
    void findExpedienteFound() {
        seedExpediente("EXP-A", "PAC-EXT-A");
        em.flush();

        Optional<ExpedienteClinico> result = gateway.findExpedienteByPacienteExternoId("PAC-EXT-A");

        assertTrue(result.isPresent());
        assertEquals("EXP-A", result.get().getId());
        assertEquals("PAC-EXT-A", result.get().getPacienteExternoId());
        assertEquals("DR-1", result.get().getDoctorResponsableId());
        assertNotNull(result.get().getCreatedAt());
    }

    @Test
    @TestTransaction
    @DisplayName("findExpedienteByPacienteExternoId: no existe → Optional vacío")
    void findExpedienteNotFound() {
        Optional<ExpedienteClinico> result = gateway.findExpedienteByPacienteExternoId("MISSING");
        assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("findConsultasByPacienteExternoId: paciente con consultas → lista ordenada DESC por fecha")
    void findConsultasOrderDesc() {
        seedExpediente("EXP-B", "PAC-EXT-B");
        seedConsulta("C-OLD", "EXP-B", LocalDateTime.of(2025, 1, 1, 9, 0));
        seedConsulta("C-NEW", "EXP-B", LocalDateTime.of(2026, 3, 15, 10, 0));
        em.flush();

        List<Consulta> result = gateway.findConsultasByPacienteExternoId("PAC-EXT-B");

        assertEquals(2, result.size());
        assertEquals("C-NEW", result.get(0).getId(), "La más reciente primero");
        assertEquals("C-OLD", result.get(1).getId());
    }

    @Test
    @TestTransaction
    @DisplayName("findConsultasByPacienteExternoId: paciente sin consultas → lista vacía")
    void findConsultasEmpty() {
        seedExpediente("EXP-C", "PAC-EXT-C");
        em.flush();

        List<Consulta> result = gateway.findConsultasByPacienteExternoId("PAC-EXT-C");

        assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("findConsultasByPacienteExternoId: pacienteExternoId inexistente → lista vacía")
    void findConsultasUnknownPatient() {
        List<Consulta> result = gateway.findConsultasByPacienteExternoId("MISSING-PAC");
        assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("findConsultasByPacienteExternoId: solo consultas del expediente del paciente")
    void findConsultasIsolatedPerExpediente() {
        seedExpediente("EXP-D", "PAC-EXT-D");
        seedExpediente("EXP-E", "PAC-EXT-E");
        seedConsulta("C-D1", "EXP-D", LocalDateTime.of(2026, 1, 1, 9, 0));
        seedConsulta("C-E1", "EXP-E", LocalDateTime.of(2026, 1, 2, 9, 0));
        em.flush();

        List<Consulta> consultasD = gateway.findConsultasByPacienteExternoId("PAC-EXT-D");

        assertEquals(1, consultasD.size());
        assertEquals("C-D1", consultasD.get(0).getId());
    }

    @Test
    @TestTransaction
    @DisplayName("findConsultaById: existe → Optional con consulta completa")
    void findConsultaByIdFound() {
        seedExpediente("EXP-F", "PAC-EXT-F");
        seedConsulta("C-F1", "EXP-F", LocalDateTime.of(2026, 2, 1, 14, 0));
        em.flush();

        Optional<Consulta> result = gateway.findConsultaById("C-F1");

        assertTrue(result.isPresent());
        Consulta c = result.get();
        assertEquals("C-F1", c.getId());
        assertEquals("EXP-F", c.getExpedienteId());
        assertEquals("motivo-C-F1", c.getMotivoConsulta());
        assertEquals("subj", c.getSubjetivo());
        assertEquals("obj", c.getObjetivo());
        assertEquals("eval", c.getEvaluacion());
        assertEquals("plan", c.getPlan());
        assertEquals("presc", c.getPrescripcion());
        assertEquals("diag", c.getDiagnostico());
    }

    @Test
    @TestTransaction
    @DisplayName("findConsultaById: no existe → Optional vacío")
    void findConsultaByIdNotFound() {
        Optional<Consulta> result = gateway.findConsultaById("MISSING-C");
        assertTrue(result.isEmpty());
    }
}
