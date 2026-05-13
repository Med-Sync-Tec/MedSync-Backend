package itesm.medsync.domain.specialty.model;

import itesm.medsync.domain.specialty.exception.InvalidSpecialtyDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpecialtyTest {

    private static final UUID VALID_ID = UUID.randomUUID();
    private static final String VALID_NOMBRE = "Cardiología";
    private static final String VALID_SLUG = "cardiologia";
    private static final String VALID_DESCRIPCION = "Diseases of the heart";

    private Specialty newValidSpecialty() {
        return new Specialty(VALID_ID, VALID_NOMBRE, VALID_SLUG, VALID_DESCRIPCION,
                true, null, null);
    }

    @Test
    @DisplayName("Constructor válido crea instancia con todos los campos")
    void constructorValid() {
        Specialty s = newValidSpecialty();

        assertEquals(VALID_ID, s.getId());
        assertEquals(VALID_NOMBRE, s.getNombre());
        assertEquals(VALID_SLUG, s.getSlug());
        assertEquals(VALID_DESCRIPCION, s.getDescripcion());
        assertTrue(s.isActivo());
        assertNull(s.getCreatedAt());
        assertNull(s.getUpdatedAt());
    }

    @Test
    @DisplayName("descripcion null es válido (campo opcional)")
    void constructorDescripcionNullOk() {
        Specialty s = new Specialty(VALID_ID, VALID_NOMBRE, VALID_SLUG, null,
                true, null, null);
        assertNull(s.getDescripcion());
    }

    @Test
    @DisplayName("id null lanza InvalidSpecialtyDataException")
    void constructorIdNull() {
        assertThrows(InvalidSpecialtyDataException.class, () -> new Specialty(
                null, VALID_NOMBRE, VALID_SLUG, VALID_DESCRIPCION, true, null, null));
    }

    @ParameterizedTest(name = "nombre inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("nombre null/blank lanza InvalidSpecialtyDataException")
    void constructorNombreBlank(String invalidNombre) {
        assertThrows(InvalidSpecialtyDataException.class, () -> new Specialty(
                VALID_ID, invalidNombre, VALID_SLUG, VALID_DESCRIPCION, true, null, null));
    }

    @Test
    @DisplayName("nombre > 100 chars lanza InvalidSpecialtyDataException")
    void constructorNombreTooLong() {
        String tooLong = "A".repeat(101);
        assertThrows(InvalidSpecialtyDataException.class, () -> new Specialty(
                VALID_ID, tooLong, VALID_SLUG, VALID_DESCRIPCION, true, null, null));
    }

    @Test
    @DisplayName("nombre exactamente 100 chars es válido")
    void constructorNombreAtLimit() {
        String exactly100 = "A".repeat(100);
        assertDoesNotThrow(() -> new Specialty(
                VALID_ID, exactly100, VALID_SLUG, VALID_DESCRIPCION, true, null, null));
    }

    @ParameterizedTest(name = "slug inválido: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("slug null/blank lanza InvalidSpecialtyDataException")
    void constructorSlugBlank(String invalidSlug) {
        assertThrows(InvalidSpecialtyDataException.class, () -> new Specialty(
                VALID_ID, VALID_NOMBRE, invalidSlug, VALID_DESCRIPCION, true, null, null));
    }

    @ParameterizedTest(name = "slug con formato inválido: [{0}]")
    @ValueSource(strings = {
            "Cardiologia",           // uppercase
            "cardio_logia",          // underscore
            "cardio logia",          // space
            "cardio--logia",         // double hyphen
            "-cardiologia",          // leading hyphen
            "cardiologia-",          // trailing hyphen
            "cardio.logia",          // dot
            "cardiología"            // accented (non a-z0-9)
    })
    @DisplayName("slug no kebab-case lanza InvalidSpecialtyDataException")
    void constructorSlugBadPattern(String invalidSlug) {
        assertThrows(InvalidSpecialtyDataException.class, () -> new Specialty(
                VALID_ID, VALID_NOMBRE, invalidSlug, VALID_DESCRIPCION, true, null, null));
    }

    @ParameterizedTest(name = "slug válido: [{0}]")
    @ValueSource(strings = {
            "cardiologia",
            "medicina-interna",
            "abc",
            "x1",
            "1a",
            "a-b-c-d"
    })
    @DisplayName("slugs en kebab-case válido son aceptados")
    void constructorSlugValid(String validSlug) {
        assertDoesNotThrow(() -> new Specialty(
                VALID_ID, VALID_NOMBRE, validSlug, VALID_DESCRIPCION, true, null, null));
    }

    @Test
    @DisplayName("slug > 60 chars lanza InvalidSpecialtyDataException")
    void constructorSlugTooLong() {
        String tooLong = "a".repeat(61);
        assertThrows(InvalidSpecialtyDataException.class, () -> new Specialty(
                VALID_ID, VALID_NOMBRE, tooLong, VALID_DESCRIPCION, true, null, null));
    }

    @Test
    @DisplayName("descripcion > 500 chars lanza InvalidSpecialtyDataException")
    void constructorDescripcionTooLong() {
        String tooLong = "A".repeat(501);
        assertThrows(InvalidSpecialtyDataException.class, () -> new Specialty(
                VALID_ID, VALID_NOMBRE, VALID_SLUG, tooLong, true, null, null));
    }

    @Test
    @DisplayName("Specialty.create genera UUID, activo=true, timestamps null")
    void factoryCreate() {
        Specialty s = Specialty.create(VALID_NOMBRE, VALID_SLUG, VALID_DESCRIPCION);

        assertNotNull(s.getId());
        assertEquals(VALID_NOMBRE, s.getNombre());
        assertEquals(VALID_SLUG, s.getSlug());
        assertEquals(VALID_DESCRIPCION, s.getDescripcion());
        assertTrue(s.isActivo());
        assertNull(s.getCreatedAt());
        assertNull(s.getUpdatedAt());
    }

    @Test
    @DisplayName("Specialty.create con descripcion null es válido")
    void factoryCreateDescripcionNull() {
        Specialty s = Specialty.create(VALID_NOMBRE, VALID_SLUG, null);
        assertNull(s.getDescripcion());
    }

    @Test
    @DisplayName("softDelete devuelve nueva instancia con activo=false, no muta original")
    void softDeleteImmutable() {
        Specialty original = newValidSpecialty();
        Specialty deleted = original.softDelete();

        assertNotSame(original, deleted);
        assertTrue(original.isActivo());
        assertFalse(deleted.isActivo());
        assertEquals(original.getId(), deleted.getId());
        assertEquals(original.getNombre(), deleted.getNombre());
        assertEquals(original.getSlug(), deleted.getSlug());
    }

    @Test
    @DisplayName("withNombre devuelve nueva instancia con nombre actualizado")
    void withNombreImmutable() {
        Specialty original = newValidSpecialty();
        Specialty renamed = original.withNombre("Cardiología Pediátrica");

        assertNotSame(original, renamed);
        assertEquals(VALID_NOMBRE, original.getNombre());
        assertEquals("Cardiología Pediátrica", renamed.getNombre());
        assertEquals(original.getId(), renamed.getId());
        assertEquals(original.getSlug(), renamed.getSlug());
    }

    @Test
    @DisplayName("withSlug devuelve nueva instancia con slug actualizado")
    void withSlugImmutable() {
        Specialty original = newValidSpecialty();
        Specialty renamed = original.withSlug("cardiologia-pediatrica");

        assertNotSame(original, renamed);
        assertEquals(VALID_SLUG, original.getSlug());
        assertEquals("cardiologia-pediatrica", renamed.getSlug());
    }

    @Test
    @DisplayName("withSlug rechaza slug inválido (regex)")
    void withSlugRejectsInvalid() {
        Specialty original = newValidSpecialty();
        assertThrows(InvalidSpecialtyDataException.class,
                () -> original.withSlug("Bad Slug"));
    }

    @Test
    @DisplayName("withDescripcion permite null")
    void withDescripcionNullable() {
        Specialty original = newValidSpecialty();
        Specialty updated = original.withDescripcion(null);

        assertNotSame(original, updated);
        assertNull(updated.getDescripcion());
        assertNotNull(original.getDescripcion());
    }

    @Test
    @DisplayName("equals/hashCode basados en id")
    void equalsAndHashCode() {
        Specialty a = new Specialty(VALID_ID, "A", "a", null, true, null, null);
        Specialty b = new Specialty(VALID_ID, "B", "b", "x", false,
                LocalDateTime.now(), LocalDateTime.now());
        Specialty c = new Specialty(UUID.randomUUID(), "A", "a", null, true, null, null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
