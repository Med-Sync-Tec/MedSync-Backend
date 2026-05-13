package itesm.medsync.infrastructure.persistence.user;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.domain.user.repository.RoleRepository;
import itesm.medsync.domain.user.repository.UserRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class UserRepositoryImplTest {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    EntityManager entityManager;

    private UUID doctorRoleId() {
        return roleRepository.findByNombre("DOCTOR")
                .map(Role::getId)
                .orElseThrow(() -> new AssertionError("DOCTOR role debe existir (seed V3)"));
    }

    @Test
    @TestTransaction
    @DisplayName("save + findByEmail: createdAt se genera, campos persisten, rol se carga via graph")
    void saveAndFindByEmail() {
        User u = User.create("Ana", "ana-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId());
        User saved = userRepository.save(u);

        assertEquals(u.getId(), saved.getId());
        assertNotNull(saved.getCreatedAt(), "createdAt debe ser asignado por Hibernate");

        entityManager.clear();

        Optional<UserWithRole> found = userRepository.findByEmail(u.getCorreo());
        assertTrue(found.isPresent());
        assertEquals("Ana", found.get().user().getNombre());
        assertEquals(doctorRoleId(), found.get().user().getRolId());
        assertTrue(found.get().user().isActivo());
        assertEquals("DOCTOR", found.get().roleName(), "Rol debe venir cargado en una sola query");
    }

    @Test
    @TestTransaction
    @DisplayName("findByEmail con correo inexistente devuelve Optional.empty()")
    void findByEmailMissing() {
        Optional<UserWithRole> result = userRepository.findByEmail(
                "no-existe-" + UUID.randomUUID() + "@tec.mx");
        assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("save sobre usuario existente actualiza (no duplica) y preserva createdAt")
    void saveUpdatesExisting() {
        User original = userRepository.save(
                User.create("Pedro", "pedro-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId()));

        User deactivated = original.deactivate();
        User updated = userRepository.save(deactivated);

        assertEquals(original.getId(), updated.getId());
        assertFalse(updated.isActivo());
        assertNotNull(updated.getCreatedAt());
    }

    @Test
    @TestTransaction
    @DisplayName("UNIQUE correo: guardar dos usuarios con mismo correo falla")
    void saveDuplicateEmailFails() {
        String email = "dup-" + UUID.randomUUID() + "@tec.mx";
        userRepository.save(User.create("A", email, null, doctorRoleId()));

        assertThrows(Exception.class,
                () -> userRepository.save(User.create("B", email, null, doctorRoleId())));
    }
}
