package itesm.medsync.domain.user.model;

import itesm.medsync.domain.user.exception.InvalidRoleDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    private static final UUID VALID_ID = UUID.randomUUID();
    private static final String VALID_NOMBRE = "DOCTOR";

    @Test
    @DisplayName("Constructor válido crea instancia con todos los campos")
    void constructorValid() {
        LocalDateTime now = LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0);
        Role r = new Role(VALID_ID, VALID_NOMBRE, "Personal médico", now);
        assertEquals(VALID_ID, r.getId());
        assertEquals(VALID_NOMBRE, r.getNombre());
        assertEquals("Personal médico", r.getDescripcion());
        assertEquals(now, r.getCreatedAt());
    }

    @Test
    @DisplayName("descripcion null es aceptado")
    void constructorDescripcionNullOk() {
        Role r = new Role(VALID_ID, VALID_NOMBRE, null, null);
        assertNull(r.getDescripcion());
    }

    @Test
    @DisplayName("id null lanza InvalidRoleDataException")
    void constructorIdNull() {
        assertThrows(InvalidRoleDataException.class,
                () -> new Role(null, VALID_NOMBRE, null, null));
    }

    @ParameterizedTest(name = "nombre inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("nombre null/blank lanza InvalidRoleDataException")
    void constructorNombreBlank(String invalid) {
        assertThrows(InvalidRoleDataException.class,
                () -> new Role(VALID_ID, invalid, null, null));
    }

    @Test
    @DisplayName("Role.create genera UUID nuevo, createdAt null")
    void factoryCreate() {
        Role r = Role.create(VALID_NOMBRE, "descripcion");
        assertNotNull(r.getId());
        assertEquals(VALID_NOMBRE, r.getNombre());
        assertEquals("descripcion", r.getDescripcion());
        assertNull(r.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode basados en id")
    void equalsAndHashCode() {
        Role a = new Role(VALID_ID, "DOCTOR", null, null);
        Role b = new Role(VALID_ID, "DIFFERENT", "other", LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0));
        Role c = new Role(UUID.randomUUID(), "DOCTOR", null, null);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
