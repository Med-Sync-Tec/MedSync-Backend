package itesm.medsync.application.specialty;

import itesm.medsync.domain.specialty.exception.DuplicateSpecialtyException;
import itesm.medsync.domain.specialty.exception.SpecialtyNotFoundException;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateSpecialtyServiceTest {

    @Mock
    SpecialtyRepository repository;

    @InjectMocks
    UpdateSpecialtyService service;

    private Specialty existing(UUID id) {
        return new Specialty(id, "Cardiología", "cardiologia", "Heart", true, null, null);
    }

    @Test
    @DisplayName("Happy path: updates all three fields")
    void updateOk() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.of(existing(id)));
        when(repository.existsByNombreActive("Cardiología Pediátrica")).thenReturn(false);
        when(repository.existsBySlugAcrossAllRows("cardiologia-pediatrica")).thenReturn(false);
        when(repository.save(any(Specialty.class))).thenAnswer(inv -> inv.getArgument(0));

        Specialty result = service.execute(id, "Cardiología Pediátrica",
                "cardiologia-pediatrica", "Pediatric heart");

        assertEquals("Cardiología Pediátrica", result.getNombre());
        assertEquals("cardiologia-pediatrica", result.getSlug());
        assertEquals("Pediatric heart", result.getDescripcion());
        assertEquals(id, result.getId());
    }

    @Test
    @DisplayName("Not found → SpecialtyNotFoundException, no save")
    void notFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.empty());

        assertThrows(SpecialtyNotFoundException.class,
                () -> service.execute(id, "X", "x", null));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Updating to same values is a no-op uniqueness-check-wise")
    void sameValues() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.of(existing(id)));
        when(repository.save(any(Specialty.class))).thenAnswer(inv -> inv.getArgument(0));

        // Uniqueness check must be skipped when value is unchanged
        Specialty result = service.execute(id, "Cardiología", "cardiologia", "Heart");

        assertEquals("Cardiología", result.getNombre());
        verify(repository, never()).existsByNombreActive(any());
        verify(repository, never()).existsBySlugAcrossAllRows(any());
    }

    @Test
    @DisplayName("New nombre conflicts with another active row → DuplicateSpecialtyException")
    void conflictNombre() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.of(existing(id)));
        when(repository.existsByNombreActive("Endocrinología")).thenReturn(true);

        DuplicateSpecialtyException ex = assertThrows(DuplicateSpecialtyException.class,
                () -> service.execute(id, "Endocrinología", "cardiologia", "Heart"));
        assertEquals("nombre", ex.getField());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("New slug conflicts with any row → DuplicateSpecialtyException")
    void conflictSlug() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.of(existing(id)));
        when(repository.existsBySlugAcrossAllRows("endocrinologia")).thenReturn(true);

        DuplicateSpecialtyException ex = assertThrows(DuplicateSpecialtyException.class,
                () -> service.execute(id, "Cardiología", "endocrinologia", "Heart"));
        assertEquals("slug", ex.getField());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Persisted entity preserves the original id and createdAt")
    void preservesIdentity() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.of(existing(id)));
        when(repository.existsByNombreActive(any())).thenReturn(false);
        when(repository.existsBySlugAcrossAllRows(any())).thenReturn(false);
        when(repository.save(any(Specialty.class))).thenAnswer(inv -> inv.getArgument(0));

        service.execute(id, "Cardiología Pediátrica", "cardiologia-pediatrica", null);

        ArgumentCaptor<Specialty> captor = ArgumentCaptor.forClass(Specialty.class);
        verify(repository).save(captor.capture());
        assertEquals(id, captor.getValue().getId());
        assertNull(captor.getValue().getDescripcion());
    }
}
