package itesm.medsync.infrastructure.persistence.pacientecontexto;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.pacientecontexto.repository.PacienteContextoRepository;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.repository.RoleRepository;
import itesm.medsync.domain.user.repository.UserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PacienteContextoRepositoryImplTest {

    @Inject
    PacienteContextoRepository repository;

    @Inject
    PatientRepository patientRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    private UUID pacienteId;

    @BeforeEach
    void setupPaciente() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId)
                .orElseThrow(() -> new AssertionError("DOCTOR role debe existir (seed V3)"));
        User medico = userRepository.save(
                User.create("Repo Medico Ctx", "ctx-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        Patient p = patientRepository.save(
                Patient.create("EXP-CTX-" + UUID.randomUUID(),
                        "Paciente Ctx",
                        LocalDate.of(1990, Month.JANUARY, 1),
                        "F",
                        medico.getId()));
        pacienteId = p.getId();
    }

    @Test
    @TestTransaction
    @DisplayName("save asigna createdAt y conserva el id generado")
    void saveAssignsTimestamp() {
        PacienteContexto ctx = PacienteContexto.create(pacienteId, TipoClinico.ENFERMEDAD, "Hipertensión");

        PacienteContexto saved = repository.save(ctx);

        assertEquals(ctx.getId(), saved.getId());
        assertNotNull(saved.getCreatedAt(), "createdAt debe asignarse por Hibernate");
        assertEquals(TipoClinico.ENFERMEDAD, saved.getTipo());
    }

    @Test
    @TestTransaction
    @DisplayName("findByUuid devuelve el contexto guardado")
    void findByUuid() {
        PacienteContexto ctx = repository.save(PacienteContexto.create(pacienteId, TipoClinico.SINTOMA, "Cefalea"));

        Optional<PacienteContexto> found = repository.findByUuid(ctx.getId());
        assertTrue(found.isPresent());
        assertEquals("Cefalea", found.get().getValor());
    }

    @Test
    @TestTransaction
    @DisplayName("findByPacienteId devuelve solo los del paciente, ordenados por createdAt desc")
    void findByPacienteIdScoped() {
        UUID otroDoctor = userRepository.save(
                User.create("Otro Medico Ctx", "ctx2-" + UUID.randomUUID() + "@tec.mx", null,
                        roleRepository.findByNombre("DOCTOR").map(Role::getId).orElseThrow()))
                .getId();
        UUID otroPaciente = patientRepository.save(
                Patient.create("EXP-OTRO-" + UUID.randomUUID(),
                        "Otro", LocalDate.of(1985, Month.MARCH, 3), "M", otroDoctor)).getId();

        repository.save(PacienteContexto.create(pacienteId, TipoClinico.ENFERMEDAD, "HTA"));
        repository.save(PacienteContexto.create(pacienteId, TipoClinico.MEDICAMENTO, "Losartán"));
        repository.save(PacienteContexto.create(otroPaciente, TipoClinico.SINTOMA, "Mareo"));

        List<PacienteContexto> mios = repository.findByPacienteId(pacienteId);
        assertEquals(2, mios.size());
        assertTrue(mios.stream().allMatch(c -> c.getPacienteId().equals(pacienteId)));
    }

    @Test
    @TestTransaction
    @DisplayName("removeById elimina el contexto")
    void removeById() {
        PacienteContexto saved = repository.save(PacienteContexto.create(pacienteId, TipoClinico.TRATAMIENTO, "Reposo"));

        repository.removeById(saved.getId());
        assertTrue(repository.findByUuid(saved.getId()).isEmpty());
    }
}
