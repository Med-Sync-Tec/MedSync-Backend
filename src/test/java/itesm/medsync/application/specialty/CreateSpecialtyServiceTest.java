package itesm.medsync.application.specialty;

import itesm.medsync.domain.specialty.exception.DuplicateSpecialtyException;
import itesm.medsync.domain.specialty.exception.InvalidSpecialtyDataException;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSpecialtyServiceTest {

    @Mock
    SpecialtyRepository repository;

    @InjectMocks
    CreateSpecialtyService service;

    @Test
    @DisplayName("Happy path: persists and returns the specialty")
    void createOk() {
        when(repository.existsByNombreActive("Cardiología")).thenReturn(false);
        when(repository.existsBySlugAcrossAllRows("cardiologia")).thenReturn(false);
        when(repository.save(any(Specialty.class))).thenAnswer(inv -> inv.getArgument(0));

        Specialty result = service.execute("Cardiología", "cardiologia", "Heart");

        assertNotNull(result);
        assertEquals("Cardiología", result.getNombre());
        assertEquals("cardiologia", result.getSlug());
        assertEquals("Heart", result.getDescripcion());
        assertTrue(result.isActivo());

        ArgumentCaptor<Specialty> captor = ArgumentCaptor.forClass(Specialty.class);
        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue().getId());
    }

    @Test
    @DisplayName("Duplicate nombre → DuplicateSpecialtyException(field=nombre), no save")
    void duplicateNombre() {
        when(repository.existsByNombreActive("Cardiología")).thenReturn(true);

        DuplicateSpecialtyException ex = assertThrows(DuplicateSpecialtyException.class,
                () -> service.execute("Cardiología", "cardiologia", null));

        assertEquals("nombre", ex.getField());
        assertEquals("Cardiología", ex.getValue());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Duplicate slug across all rows → DuplicateSpecialtyException(field=slug)")
    void duplicateSlug() {
        when(repository.existsByNombreActive("Cardiología")).thenReturn(false);
        when(repository.existsBySlugAcrossAllRows("cardiologia")).thenReturn(true);

        DuplicateSpecialtyException ex = assertThrows(DuplicateSpecialtyException.class,
                () -> service.execute("Cardiología", "cardiologia", null));

        assertEquals("slug", ex.getField());
        assertEquals("cardiologia", ex.getValue());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Invalid domain data propagates InvalidSpecialtyDataException")
    void invalidData() {
        when(repository.existsByNombreActive(any())).thenReturn(false);
        when(repository.existsBySlugAcrossAllRows(any())).thenReturn(false);

        assertThrows(InvalidSpecialtyDataException.class,
                () -> service.execute("Cardiología", "Bad Slug!", null));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("descripcion null is accepted")
    void descripcionNull() {
        when(repository.existsByNombreActive(any())).thenReturn(false);
        when(repository.existsBySlugAcrossAllRows(any())).thenReturn(false);
        when(repository.save(any(Specialty.class))).thenAnswer(inv -> inv.getArgument(0));

        Specialty s = service.execute("Neurología", "neurologia", null);
        assertNull(s.getDescripcion());
    }
}
