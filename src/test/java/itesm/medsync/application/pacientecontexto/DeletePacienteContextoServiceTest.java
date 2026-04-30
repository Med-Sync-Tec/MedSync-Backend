package itesm.medsync.application.pacientecontexto;

import itesm.medsync.domain.pacientecontexto.exception.PacienteContextoNotFoundException;
import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.pacientecontexto.repository.PacienteContextoRepository;
import itesm.medsync.domain.shared.model.TipoClinico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletePacienteContextoServiceTest {

    @Mock
    PacienteContextoRepository repository;

    @InjectMocks
    DeletePacienteContextoService service;

    @Test
    @DisplayName("Existente: invoca removeById")
    void deleteOk() {
        UUID id = UUID.randomUUID();
        PacienteContexto ctx = PacienteContexto.create(UUID.randomUUID(), TipoClinico.SINTOMA, "x");
        when(repository.findByUuid(id)).thenReturn(Optional.of(ctx));

        service.execute(id);
        verify(repository).removeById(id);
    }

    @Test
    @DisplayName("Inexistente: PacienteContextoNotFoundException, no borra")
    void deleteNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.empty());

        assertThrows(PacienteContextoNotFoundException.class, () -> service.execute(id));
        verify(repository, never()).removeById(any());
    }
}
