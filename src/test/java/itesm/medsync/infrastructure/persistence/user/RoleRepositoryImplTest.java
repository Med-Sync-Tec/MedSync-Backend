package itesm.medsync.infrastructure.persistence.user;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.repository.RoleRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RoleRepositoryImplTest {

    @Inject
    RoleRepository repository;

    @Test
    @TestTransaction
    @DisplayName("findByNombre('DOCTOR') devuelve el rol sembrado por V3")
    void findSeededDoctorRole() {
        Optional<Role> role = repository.findByNombre("DOCTOR");
        assertTrue(role.isPresent(), "DOCTOR debe existir (seed V3)");
        assertEquals("DOCTOR", role.get().getNombre());
        assertNotNull(role.get().getId());
    }

    @Test
    @TestTransaction
    @DisplayName("findByNombre('COO') devuelve el rol sembrado por V3")
    void findSeededCooRole() {
        Optional<Role> role = repository.findByNombre("COO");
        assertTrue(role.isPresent(), "COO debe existir (seed V3)");
        assertEquals("COO", role.get().getNombre());
    }

    @Test
    @TestTransaction
    @DisplayName("findByNombre con rol inexistente devuelve Optional.empty()")
    void findByNombreMissing() {
        Optional<Role> role = repository.findByNombre("NOPE");
        assertTrue(role.isEmpty());
    }
}
