package itesm.medsync.infrastructure.persistence.patient;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
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
class PatientRepositoryImplTest {

    @Inject
    PatientRepository repository;

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    private UUID medicoId;

    @BeforeEach
    void setupMedico() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId)
                .orElseThrow(() -> new AssertionError("DOCTOR role debe existir (seed V3)"));
        User medico = userRepository.save(
                User.create("Repo Medico", "repo-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        medicoId = medico.getId();
    }

    private Patient newPatient(String expediente) {
        return Patient.create(expediente, "Nombre " + expediente,
                LocalDate.of(1990, Month.JANUARY, 1), "M", medicoId);
    }

    @Test
    @TestTransaction
    @DisplayName("save + findById: timestamps se generan")
    void saveAndFind() {
        Patient p = newPatient("EXP-A");
        Patient saved = repository.save(p);

        assertEquals(p.getId(), saved.getId());
        assertNotNull(saved.getCreatedAt(), "createdAt debe ser asignado por Hibernate");
        assertNotNull(saved.getUpdatedAt(), "updatedAt debe ser asignado por Hibernate");

        Optional<Patient> found = repository.findByUuid(p.getId());
        assertTrue(found.isPresent());
        assertEquals("EXP-A", found.get().getExpedienteExternoId());
        assertTrue(found.get().isActivo());
    }

    @Test
    @TestTransaction
    @DisplayName("findAllActive no incluye soft-deleted")
    void findAllActiveExcludesSoftDeleted() {
        Patient active = repository.save(newPatient("EXP-ACT"));
        Patient toDelete = repository.save(newPatient("EXP-DEL"));
        repository.save(toDelete.softDelete());

        List<Patient> activos = repository.findAllActive();
        assertTrue(activos.stream().anyMatch(p -> p.getId().equals(active.getId())));
        assertFalse(activos.stream().anyMatch(p -> p.getId().equals(toDelete.getId())));
    }

    @Test
    @TestTransaction
    @DisplayName("findById sobre soft-deleted devuelve empty (por @SQLRestriction)")
    void findByIdSoftDeletedReturnsEmpty() {
        Patient p = repository.save(newPatient("EXP-SD"));
        repository.save(p.softDelete());

        Optional<Patient> result = repository.findByUuid(p.getId());
        assertTrue(result.isEmpty(), "@SQLRestriction debe filtrar soft-deleted");
    }

    @Test
    @TestTransaction
    @DisplayName("existsByExpedienteExternoId detecta activos")
    void existsByExpedienteActive() {
        repository.save(newPatient("EXP-EX"));
        assertTrue(repository.existsByExpedienteExternoId("EXP-EX"));
        assertFalse(repository.existsByExpedienteExternoId("NOPE"));
    }

    @Test
    @TestTransaction
    @DisplayName("existsByExpedienteExternoId devuelve true incluso si el paciente está soft-deleted")
    void existsByExpedienteSoftDeletedBypassSqlRestriction() {
        Patient p = repository.save(newPatient("EXP-SD-EXISTS"));
        repository.save(p.softDelete());

        assertTrue(repository.existsByExpedienteExternoId("EXP-SD-EXISTS"),
                "Query nativa debe detectar soft-deleted para prevenir UNIQUE violation");
    }

    @Test
    @TestTransaction
    @DisplayName("save de Patient inexistente hace insert; save sobre existente hace update")
    void saveInsertThenUpdate() {
        Patient p = repository.save(newPatient("EXP-UP"));
        Patient deleted = repository.save(p.softDelete());

        assertEquals(p.getId(), deleted.getId());
        assertFalse(deleted.isActivo());
    }
}
