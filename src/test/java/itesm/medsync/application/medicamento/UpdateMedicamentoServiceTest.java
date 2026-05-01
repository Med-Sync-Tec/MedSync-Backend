package itesm.medsync.application.medicamento;

import itesm.medsync.domain.medicamento.exception.DuplicateMedicamentoException;
import itesm.medsync.domain.medicamento.exception.EstadoNotFoundException;
import itesm.medsync.domain.medicamento.exception.MedicamentoNotFoundException;
import itesm.medsync.domain.medicamento.model.Medicamento;
import itesm.medsync.domain.medicamento.model.MedicamentoEstado;
import itesm.medsync.domain.medicamento.model.MedicamentoEstadoNames;
import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;
import itesm.medsync.domain.medicamento.repository.MedicamentoEstadoRepository;
import itesm.medsync.domain.medicamento.repository.MedicamentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateMedicamentoServiceTest {

    @Mock
    MedicamentoRepository medicamentoRepository;

    @Mock
    MedicamentoEstadoRepository estadoRepository;

    @InjectMocks
    UpdateMedicamentoService service;

    @Test
    @DisplayName("Medicamento inexistente: lanza MedicamentoNotFoundException, no actualiza")
    void updateNotFound() {
        UUID id = UUID.randomUUID();
        when(medicamentoRepository.findByUuid(id)).thenReturn(Optional.empty());

        assertThrows(MedicamentoNotFoundException.class,
                () -> service.execute(id, "X", MedicamentoEstadoNames.VIGENTE, "d"));

        verify(medicamentoRepository, never()).update(any());
    }

    @Test
    @DisplayName("Renombrar a un nombre que ya pertenece a otro medicamento: 409")
    void updateDuplicateName() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID estadoId = UUID.randomUUID();
        MedicamentoEstado vigente = new MedicamentoEstado(estadoId, MedicamentoEstadoNames.VIGENTE, null);
        Medicamento current = new Medicamento(id, "Old", estadoId, "d", null, null);
        Medicamento conflict = new Medicamento(otherId, "Taken", estadoId, "d", null, null);

        when(medicamentoRepository.findByUuid(id))
                .thenReturn(Optional.of(new MedicamentoWithEstado(current, vigente)));
        when(medicamentoRepository.findByNombre("Taken"))
                .thenReturn(Optional.of(new MedicamentoWithEstado(conflict, vigente)));

        assertThrows(DuplicateMedicamentoException.class,
                () -> service.execute(id, "Taken", MedicamentoEstadoNames.VIGENTE, "d"));

        verify(medicamentoRepository, never()).update(any());
    }

    @Test
    @DisplayName("Renombrar al mismo nombre actual: permitido (no es duplicado)")
    void updateSameName() {
        UUID id = UUID.randomUUID();
        UUID estadoId = UUID.randomUUID();
        MedicamentoEstado vigente = new MedicamentoEstado(estadoId, MedicamentoEstadoNames.VIGENTE, null);
        Medicamento current = new Medicamento(id, "Same", estadoId, "d", null, null);

        when(medicamentoRepository.findByUuid(id))
                .thenReturn(Optional.of(new MedicamentoWithEstado(current, vigente)));
        when(medicamentoRepository.findByNombre("Same"))
                .thenReturn(Optional.of(new MedicamentoWithEstado(current, vigente)));
        when(estadoRepository.findByNombre(MedicamentoEstadoNames.VIGENTE)).thenReturn(Optional.of(vigente));

        assertDoesNotThrow(() -> service.execute(id, "Same", MedicamentoEstadoNames.VIGENTE, "new"));

        verify(medicamentoRepository).update(any());
    }

    @Test
    @DisplayName("Estado desconocido: lanza EstadoNotFoundException")
    void updateUnknownEstado() {
        UUID id = UUID.randomUUID();
        UUID estadoId = UUID.randomUUID();
        MedicamentoEstado vigente = new MedicamentoEstado(estadoId, MedicamentoEstadoNames.VIGENTE, null);
        Medicamento current = new Medicamento(id, "X", estadoId, "d", null, null);

        when(medicamentoRepository.findByUuid(id))
                .thenReturn(Optional.of(new MedicamentoWithEstado(current, vigente)));
        when(medicamentoRepository.findByNombre("X"))
                .thenReturn(Optional.of(new MedicamentoWithEstado(current, vigente)));
        when(estadoRepository.findByNombre("inexistente")).thenReturn(Optional.empty());

        assertThrows(EstadoNotFoundException.class,
                () -> service.execute(id, "X", "inexistente", "d"));

        verify(medicamentoRepository, never()).update(any());
    }
}
