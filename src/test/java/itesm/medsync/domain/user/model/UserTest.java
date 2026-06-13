package itesm.medsync.domain.user.model;

import itesm.medsync.domain.user.exception.InvalidUserDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private static final UUID VALID_ID = UUID.randomUUID();
    private static final UUID VALID_ROL_ID = UUID.randomUUID();
    private static final String VALID_NOMBRE = "Juan Perez";
    private static final String VALID_CORREO = "juan@tec.mx";

    private User newValidUser() {
        return new User(VALID_ID, VALID_NOMBRE, VALID_CORREO, null, VALID_ROL_ID, true, null);
    }

    @Test
    @DisplayName("Constructor válido crea instancia con todos los campos")
    void constructorValid() {
        LocalDateTime now = LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0);
        User u = new User(VALID_ID, VALID_NOMBRE, VALID_CORREO, null, VALID_ROL_ID, true, now);
        assertEquals(VALID_ID, u.getId());
        assertEquals(VALID_NOMBRE, u.getNombre());
        assertEquals(VALID_CORREO, u.getCorreo());
        assertEquals(VALID_ROL_ID, u.getRolId());
        assertTrue(u.isActivo());
        assertEquals(now, u.getCreatedAt());
    }

    @Test
    @DisplayName("id null lanza InvalidUserDataException")
    void constructorIdNull() {
        assertThrows(InvalidUserDataException.class,
                () -> new User(null, VALID_NOMBRE, VALID_CORREO, null, VALID_ROL_ID, true, null));
    }

    @ParameterizedTest(name = "nombre inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("nombre null/blank lanza InvalidUserDataException")
    void constructorNombreBlank(String invalid) {
        assertThrows(InvalidUserDataException.class,
                () -> new User(VALID_ID, invalid, VALID_CORREO, null, VALID_ROL_ID, true, null));
    }

    @ParameterizedTest(name = "correo inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "noatsign", "@noleft.com", "noright@"})
    @DisplayName("correo null/blank/malformado lanza InvalidUserDataException")
    void constructorCorreoInvalid(String invalid) {
        assertThrows(InvalidUserDataException.class,
                () -> new User(VALID_ID, VALID_NOMBRE, invalid, null, VALID_ROL_ID, true, null));
    }

    @Test
    @DisplayName("rolId null lanza InvalidUserDataException")
    void constructorRolIdNull() {
        assertThrows(InvalidUserDataException.class,
                () -> new User(VALID_ID, VALID_NOMBRE, VALID_CORREO, null, null, true, null));
    }

    @Test
    @DisplayName("User.create genera UUID nuevo, activo=true, createdAt null")
    void factoryCreate() {
        User u = User.create(VALID_NOMBRE, VALID_CORREO, null, VALID_ROL_ID);
        assertNotNull(u.getId());
        assertEquals(VALID_NOMBRE, u.getNombre());
        assertEquals(VALID_CORREO, u.getCorreo());
        assertEquals(VALID_ROL_ID, u.getRolId());
        assertTrue(u.isActivo());
        assertNull(u.getCreatedAt());
    }

    @Test
    @DisplayName("deactivate devuelve nueva instancia con activo=false, no muta original")
    void deactivateImmutable() {
        User original = newValidUser();
        User deactivated = original.deactivate();

        assertNotSame(original, deactivated);
        assertTrue(original.isActivo(), "Original NO debe mutar");
        assertFalse(deactivated.isActivo());
        assertEquals(original.getId(), deactivated.getId());
        assertEquals(original.getCorreo(), deactivated.getCorreo());
    }

    @Test
    @DisplayName("equals/hashCode basados en id")
    void equalsAndHashCode() {
        User a = new User(VALID_ID, "Juan", "juan@tec.mx", null, VALID_ROL_ID, true, null);
        User b = new User(VALID_ID, "Otro", "otro@tec.mx", null, UUID.randomUUID(), false, LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0));
        User c = new User(UUID.randomUUID(), "Juan", "juan@tec.mx", null, VALID_ROL_ID, true, null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
