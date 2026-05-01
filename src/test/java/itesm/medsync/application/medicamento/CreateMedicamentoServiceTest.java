package itesm.medsync.application.medicamento;

import itesm.medsync.domain.medicamento.exception.DuplicateMedicamentoException;
import itesm.medsync.domain.medicamento.exception.EstadoNotFoundException;
import itesm.medsync.domain.medicamento.model.Medicamento;
import itesm.medsync.domain.medicamento.model.MedicamentoEstado;
import itesm.medsync.domain.medicamento.model.MedicamentoEstadoNames;
import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;
import itesm.medsync.domain.medicamento.repository.MedicamentoEstadoRepository;
import itesm.medsync.domain.medicamento.repository.MedicamentoRepository;
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
class CreateMedicamentoServiceTest {

    @Mock
    MedicamentoRepository medicamentoRepository;

    @Mock
    MedicamentoEstadoRepository estadoRepository;

    @InjectMocks
    CreateMedicamentoService service;

    private MedicamentoEstado vigenteEstado() {
        return new MedicamentoEstado(UUID.randomUUID(), MedicamentoEstadoNames.VIGENTE, "vigente");
    }

    @Test
    @DisplayName("Happy path: usa estado vigente y guarda medicamento")
    void createOk() {
        MedicamentoEstado vigente = vigenteEstado();
        when(medicamentoRepository.findByNombre("Metformina")).thenReturn(Optional.empty());
        when(estadoRepository.findByNombre(MedicamentoEstadoNames.VIGENTE)).thenReturn(Optional.of(vigente));
        when(medicamentoRepository.save(any(Medicamento.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicamentoWithEstado result = service.execute("Metformina", "DM2");

        assertNotNull(result);
        assertEquals("Metformina", result.medicamento().getNombre());
        assertEquals(vigente.getId(), result.estado().getId());

        ArgumentCaptor<Medicamento> captor = ArgumentCaptor.forClass(Medicamento.class);
        verify(medicamentoRepository).save(captor.capture());
        assertEquals(vigente.getId(), captor.getValue().getEstadoId());
    }

    @Test
    @DisplayName("Nombre duplicado: lanza DuplicateMedicamentoException, no guarda")
    void createDuplicate() {
        MedicamentoEstado vigente = vigenteEstado();
        Medicamento existing = Medicamento.create("Metformina", vigente.getId(), "x");
        when(medicamentoRepository.findByNombre("Metformina"))
                .thenReturn(Optional.of(new MedicamentoWithEstado(existing, vigente)));

        assertThrows(DuplicateMedicamentoException.class,
                () -> service.execute("Metformina", "DM2"));

        verify(medicamentoRepository, never()).save(any());
        verify(estadoRepository, never()).findByNombre(any());
    }

    @Test
    @DisplayName("Estado vigente faltante en BD: lanza EstadoNotFoundException")
    void createDefaultEstadoMissing() {
        when(medicamentoRepository.findByNombre("Metformina")).thenReturn(Optional.empty());
        when(estadoRepository.findByNombre(MedicamentoEstadoNames.VIGENTE)).thenReturn(Optional.empty());

        assertThrows(EstadoNotFoundException.class,
                () -> service.execute("Metformina", "DM2"));

        verify(medicamentoRepository, never()).save(any());
    }
}
