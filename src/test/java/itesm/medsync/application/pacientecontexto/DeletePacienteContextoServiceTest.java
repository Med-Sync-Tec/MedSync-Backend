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
    @DisplayName("Existente y pertenece al paciente: invoca removeById")
    void deleteOk() {
        UUID patientId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        PacienteContexto ctx = PacienteContexto.create(patientId, TipoClinico.SINTOMA, "x");
        when(repository.findByUuid(id)).thenReturn(Optional.of(ctx));

        service.execute(patientId, id);
        verify(repository).removeById(id);
    }

    @Test
    @DisplayName("Inexistente: PacienteContextoNotFoundException, no borra")
    void deleteNotFound() {
        UUID patientId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(repository.findByUuid(id)).thenReturn(Optional.empty());

        assertThrows(PacienteContextoNotFoundException.class, () -> service.execute(patientId, id));
        verify(repository, never()).removeById(any());
    }

    @Test
    @DisplayName("Contexto pertenece a otro paciente: 404 (no enumeration), no borra")
    void deleteWrongOwner() {
        UUID requestedPatientId = UUID.randomUUID();
        UUID actualPatientId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        PacienteContexto ctx = PacienteContexto.create(actualPatientId, TipoClinico.SINTOMA, "x");
        when(repository.findByUuid(id)).thenReturn(Optional.of(ctx));

        assertThrows(PacienteContextoNotFoundException.class,
                () -> service.execute(requestedPatientId, id));
        verify(repository, never()).removeById(any());
    }
}
