package itesm.medsync.domain.medicamento.model;

import itesm.medsync.domain.medicamento.exception.InvalidMedicamentoDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MedicamentoTest {

    @Test
    @DisplayName("create asigna id y deja timestamps null (los pone Hibernate)")
    void createAssignsId() {
        UUID estadoId = UUID.randomUUID();
        Medicamento m = Medicamento.create("Metformina 850mg", estadoId, "Antidiabético");

        assertNotNull(m.getId());
        assertEquals("Metformina 850mg", m.getNombre());
        assertEquals(estadoId, m.getEstadoId());
        assertNull(m.getCreatedAt());
    }

    @Test
    @DisplayName("nombre blank lanza InvalidMedicamentoDataException")
    void nombreBlankThrows() {
        UUID estadoId = UUID.randomUUID();
        assertThrows(InvalidMedicamentoDataException.class,
                () -> Medicamento.create("   ", estadoId, "x"));
    }

    @Test
    @DisplayName("estadoId null lanza InvalidMedicamentoDataException")
    void estadoIdNullThrows() {
        assertThrows(InvalidMedicamentoDataException.class,
                () -> Medicamento.create("Metformina", null, "x"));
    }

    @Test
    @DisplayName("withEstado conserva id/nombre y cambia estadoId (inmutable)")
    void withEstadoReturnsCopy() {
        UUID estado1 = UUID.randomUUID();
        UUID estado2 = UUID.randomUUID();
        Medicamento original = Medicamento.create("X", estado1, "d");

        Medicamento changed = original.withEstado(estado2);

        assertEquals(original.getId(), changed.getId());
        assertEquals(original.getNombre(), changed.getNombre());
        assertEquals(estado2, changed.getEstadoId());
        assertEquals(estado1, original.getEstadoId(), "original no se muta");
    }

    @Test
    @DisplayName("update conserva id y aplica nuevos campos")
    void updateReturnsCopy() {
        UUID estado1 = UUID.randomUUID();
        UUID estado2 = UUID.randomUUID();
        Medicamento original = Medicamento.create("X", estado1, "old");

        Medicamento updated = original.update("Y", estado2, "new");

        assertEquals(original.getId(), updated.getId());
        assertEquals("Y", updated.getNombre());
        assertEquals(estado2, updated.getEstadoId());
        assertEquals("new", updated.getDescripcion());
    }
}
