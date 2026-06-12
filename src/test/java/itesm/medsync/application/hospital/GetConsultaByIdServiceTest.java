package itesm.medsync.application.hospital;

import itesm.medsync.domain.hospital.exception.ConsultaNotFoundException;
import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.repository.HospitalGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetConsultaByIdServiceTest {

    @Mock
    HospitalGateway hospitalGateway;

    @InjectMocks
    GetConsultaByIdService service;

    @Test
    @DisplayName("Consulta existente → devuelve la consulta")
    void found() {
        Consulta c = new Consulta("C-1", "EXP-1", LocalDateTime.of(2025, 1, 15, 10, 0),
                null, null, null, null, null, null, null, null, null);
        when(hospitalGateway.findConsultaById("C-1")).thenReturn(Optional.of(c));

        Consulta result = service.execute("C-1");

        assertEquals(c, result);
    }

    @Test
    @DisplayName("Consulta no existente → ConsultaNotFoundException")
    void notFound() {
        when(hospitalGateway.findConsultaById("missing")).thenReturn(Optional.empty());

        assertThrows(ConsultaNotFoundException.class, () -> service.execute("missing"));
    }
}
