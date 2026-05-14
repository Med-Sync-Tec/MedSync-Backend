package itesm.medsync.infrastructure.persistence.specialty;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SpecialtyRepositoryImplTest {

    @Inject
    SpecialtyRepository repository;

    private Specialty newSpecialty(String nombre, String slug) {
        return Specialty.create(nombre, slug, "Test descripcion");
    }

    @Test
    @TestTransaction
    @DisplayName("save + findByUuid: timestamps are assigned")
    void saveAndFind() {
        Specialty s = newSpecialty("Test Specialty", "test-specialty-uniq-1");
        Specialty saved = repository.save(s);

        assertEquals(s.getId(), saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<Specialty> found = repository.findByUuid(s.getId());
        assertTrue(found.isPresent());
        assertEquals("Test Specialty", found.get().getNombre());
        assertTrue(found.get().isActivo());
    }

    @Test
    @TestTransaction
    @DisplayName("findBySlug resolves a seeded slug")
    void findBySlugSeeded() {
        Optional<Specialty> cardio = repository.findBySlug("cardiologia");
        assertTrue(cardio.isPresent(), "V11 seed must include cardiologia");
        assertEquals("Cardiología", cardio.get().getNombre());
    }

    @Test
    @TestTransaction
    @DisplayName("findAllActive includes the 16 seeded rows ordered by nombre")
    void findAllActiveOrdered() {
        List<Specialty> all = repository.findAllActive();
        assertTrue(all.size() >= 16, "should at least include 16 seeded specialties");
        // Ordered alphabetically — Cardiología < Dermatología
        int cardio = indexOfNombre(all, "Cardiología");
        int derma = indexOfNombre(all, "Dermatología");
        assertTrue(cardio >= 0 && derma > cardio, "alphabetic order broken");
    }

    @Test
    @TestTransaction
    @DisplayName("@SQLRestriction hides soft-deleted rows from findByUuid")
    void softDeleteHidden() {
        Specialty s = repository.save(newSpecialty("Temp", "temp-slug-soft-1"));
        repository.save(s.softDelete());

        assertTrue(repository.findByUuid(s.getId()).isEmpty(),
                "@SQLRestriction must hide soft-deleted rows");
    }

    @Test
    @TestTransaction
    @DisplayName("existsByNombreActive returns false for a nombre present only on a soft-deleted row")
    void existsByNombreActiveSkipsSoftDeleted() {
        Specialty s = repository.save(newSpecialty("Ghost Specialty", "ghost-slug-1"));
        repository.save(s.softDelete());

        assertFalse(repository.existsByNombreActive("Ghost Specialty"),
                "soft-deleted nombre must be free for reuse");
    }

    @Test
    @TestTransaction
    @DisplayName("existsBySlugAcrossAllRows returns true for a slug present only on a soft-deleted row")
    void existsBySlugAllIncludesSoftDeleted() {
        Specialty s = repository.save(newSpecialty("Other", "ghost-slug-keep-2"));
        repository.save(s.softDelete());

        assertTrue(repository.existsBySlugAcrossAllRows("ghost-slug-keep-2"),
                "slug uniqueness must be global, including soft-deleted rows");
    }

    @Test
    @TestTransaction
    @DisplayName("existsByNombreActive is true for an active row")
    void existsByNombreActiveTrue() {
        repository.save(newSpecialty("Exists Nombre", "exists-nombre-slug-1"));
        assertTrue(repository.existsByNombreActive("Exists Nombre"));
    }

    @Test
    @TestTransaction
    @DisplayName("save acts as upsert on the same id (update path)")
    void saveUpdatesExisting() {
        Specialty s = repository.save(newSpecialty("Old Name", "old-slug-up-1"));
        Specialty renamed = s.withNombre("New Name");
        Specialty updated = repository.save(renamed);

        assertEquals(s.getId(), updated.getId());
        assertEquals("New Name", updated.getNombre());
    }

    private static int indexOfNombre(List<Specialty> list, String nombre) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getNombre().equals(nombre)) return i;
        }
        return -1;
    }
}
