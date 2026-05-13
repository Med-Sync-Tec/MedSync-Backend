package itesm.medsync.application.specialty;

import itesm.medsync.domain.specialty.exception.SpecialtyNotFoundException;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSpecialtyByIdServiceTest {

    @Mock
    SpecialtyRepository repository;

    @InjectMocks
    GetSpecialtyByIdService service;

    @Test
    @DisplayName("Found → returns specialty")
    void found() {
        UUID id = UUID.randomUUID();
        Specialty s = new Specialty(id, "Cardiología", "cardiologia", null, true, null, null);
        when(repository.findByUuid(id)).thenReturn(Optional.of(s));

        Specialty result = service.execute(id);

        assertSame(s, result);
    }

    @Test
    @DisplayName("Not found → SpecialtyNotFoundException")
    void notFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.empty());

        SpecialtyNotFoundException ex = assertThrows(SpecialtyNotFoundException.class,
                () -> service.execute(id));
        assertTrue(ex.getMessage().contains(id.toString()));
    }
}
