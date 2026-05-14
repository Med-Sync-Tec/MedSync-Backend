package itesm.medsync.application.specialty;

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
class SoftDeleteSpecialtyServiceTest {

    @Mock
    SpecialtyRepository repository;

    @InjectMocks
    SoftDeleteSpecialtyService service;

    @Test
    @DisplayName("Happy path: persists row with activo=false")
    void deleteOk() {
        UUID id = UUID.randomUUID();
        Specialty active = new Specialty(id, "Cardiología", "cardiologia", null, true, null, null);
        when(repository.findByUuid(id)).thenReturn(Optional.of(active));

        service.execute(id);

        ArgumentCaptor<Specialty> captor = ArgumentCaptor.forClass(Specialty.class);
        verify(repository).save(captor.capture());
        assertFalse(captor.getValue().isActivo());
        assertEquals(id, captor.getValue().getId());
    }

    @Test
    @DisplayName("Not found → SpecialtyNotFoundException, no save")
    void notFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.empty());

        assertThrows(SpecialtyNotFoundException.class, () -> service.execute(id));
        verify(repository, never()).save(any());
    }
}
