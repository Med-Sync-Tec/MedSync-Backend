package itesm.medsync.application.specialty;

import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListActiveSpecialtiesServiceTest {

    @Mock
    SpecialtyRepository repository;

    @InjectMocks
    ListActiveSpecialtiesService service;

    @Test
    @DisplayName("Delegates to repository.findAllActive() and returns its result")
    void delegates() {
        Specialty a = new Specialty(UUID.randomUUID(), "Cardiología", "cardiologia",
                null, true, null, null);
        Specialty b = new Specialty(UUID.randomUUID(), "Endocrinología", "endocrinologia",
                null, true, null, null);
        when(repository.findAllActive()).thenReturn(List.of(a, b));

        List<Specialty> result = service.execute();

        assertEquals(2, result.size());
        assertSame(a, result.get(0));
        assertSame(b, result.get(1));
    }

    @Test
    @DisplayName("Empty list propagates as empty")
    void emptyList() {
        when(repository.findAllActive()).thenReturn(List.of());
        assertTrue(service.execute().isEmpty());
    }
}
