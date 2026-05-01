package itesm.medsync.infrastructure.persistence.medicamento;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import itesm.medsync.domain.medicamento.exception.EstadoNotFoundException;
import itesm.medsync.domain.medicamento.exception.MedicamentoNotFoundException;
import itesm.medsync.domain.medicamento.model.Medicamento;
import itesm.medsync.domain.medicamento.model.MedicamentoEstado;
import itesm.medsync.domain.medicamento.model.MedicamentoEstadoNames;
import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;
import itesm.medsync.domain.medicamento.model.MedicamentosPage;
import itesm.medsync.domain.medicamento.repository.MedicamentoEstadoRepository;
import itesm.medsync.domain.medicamento.repository.MedicamentoRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MedicamentoRepositoryImplTest {

    @Inject
    MedicamentoRepository repository;

    @Inject
    MedicamentoEstadoRepository estadoRepository;

    private UUID vigenteId() {
        return estadoRepository.findByNombre(MedicamentoEstadoNames.VIGENTE)
                .map(MedicamentoEstado::getId)
                .orElseThrow(() -> new AssertionError("Estado 'vigente' debe existir (seed V8)"));
    }

    @Test
    @TestTransaction
    @DisplayName("save persiste el medicamento y findByUuid lo recupera con su estado")
    void saveAndFind() {
        UUID estadoId = vigenteId();
        Medicamento m = Medicamento.create("TEST-METFORMINA-" + UUID.randomUUID(), estadoId, "desc");

        repository.save(m);

        Optional<MedicamentoWithEstado> found = repository.findByUuid(m.getId());
        assertTrue(found.isPresent());
        assertEquals(m.getId(), found.get().medicamento().getId());
        assertEquals(MedicamentoEstadoNames.VIGENTE, found.get().estado().getNombre());
    }

    @Test
    @TestTransaction
    @DisplayName("save con estado inexistente lanza EstadoNotFoundException")
    void saveUnknownEstado() {
        Medicamento m = Medicamento.create("TEST-X-" + UUID.randomUUID(), UUID.randomUUID(), null);
        assertThrows(EstadoNotFoundException.class, () -> repository.save(m));
    }

    @Test
    @TestTransaction
    @DisplayName("findByNombre devuelve el medicamento exacto")
    void findByNombre() {
        UUID estadoId = vigenteId();
        String nombre = "TEST-AMOX-" + UUID.randomUUID();
        repository.save(Medicamento.create(nombre, estadoId, "x"));

        Optional<MedicamentoWithEstado> found = repository.findByNombre(nombre);
        assertTrue(found.isPresent());
        assertEquals(nombre, found.get().medicamento().getNombre());
    }

    @Test
    @TestTransaction
    @DisplayName("findPaginated filtra por nombre (case-insensitive partial match)")
    void findPaginatedByNombre() {
        UUID estadoId = vigenteId();
        String prefix = "ZZTESTX-" + UUID.randomUUID().toString().substring(0, 8);
        repository.save(Medicamento.create(prefix + "-A", estadoId, "x"));
        repository.save(Medicamento.create(prefix + "-B", estadoId, "x"));

        MedicamentosPage page = repository.findPaginated(prefix.toLowerCase(), null, 0, 10);

        assertEquals(2, page.totalElements());
        assertEquals(2, page.content().size());
    }

    @Test
    @TestTransaction
    @DisplayName("findPaginated filtra por estado")
    void findPaginatedByEstado() {
        UUID vigente = vigenteId();
        UUID obsoleto = estadoRepository.findByNombre(MedicamentoEstadoNames.OBSOLETO)
                .map(MedicamentoEstado::getId)
                .orElseThrow();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        repository.save(Medicamento.create("ZZA-" + suffix, vigente, "x"));
        repository.save(Medicamento.create("ZZB-" + suffix, obsoleto, "x"));

        MedicamentosPage page = repository.findPaginated("ZZ", MedicamentoEstadoNames.OBSOLETO, 0, 10);

        assertTrue(page.content().stream()
                .allMatch(m -> MedicamentoEstadoNames.OBSOLETO.equals(m.estado().getNombre())));
    }

    @Test
    @TestTransaction
    @DisplayName("findPaginated respeta page y size")
    void findPaginatedPageSize() {
        UUID estadoId = vigenteId();
        String prefix = "ZZPAGE-" + UUID.randomUUID().toString().substring(0, 6);
        for (int i = 0; i < 5; i++) {
            repository.save(Medicamento.create(prefix + "-" + i, estadoId, "x"));
        }

        MedicamentosPage page0 = repository.findPaginated(prefix, null, 0, 2);
        MedicamentosPage page1 = repository.findPaginated(prefix, null, 1, 2);

        assertEquals(5, page0.totalElements());
        assertEquals(3, page0.totalPages());
        assertEquals(2, page0.content().size());
        assertEquals(2, page1.content().size());
    }

    @Test
    @TestTransaction
    @DisplayName("update aplica cambios; update con id inexistente lanza MedicamentoNotFoundException")
    void update() {
        UUID estadoId = vigenteId();
        Medicamento original = Medicamento.create("ZZUPD-" + UUID.randomUUID(), estadoId, "old");
        repository.save(original);

        Medicamento updated = original.update("ZZUPD-renamed-" + UUID.randomUUID(), estadoId, "new");
        repository.update(updated);

        Optional<MedicamentoWithEstado> found = repository.findByUuid(original.getId());
        assertTrue(found.isPresent());
        assertEquals("new", found.get().medicamento().getDescripcion());

        Medicamento ghost = Medicamento.create("phantom", estadoId, null);
        assertThrows(MedicamentoNotFoundException.class, () -> repository.update(ghost));
    }

    @Test
    @TestTransaction
    @DisplayName("findByUuid de un id inexistente devuelve Optional.empty")
    void findByUuidNotFound() {
        assertTrue(repository.findByUuid(UUID.randomUUID()).isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("delete remueve el medicamento")
    void deleteById() {
        UUID estadoId = vigenteId();
        Medicamento m = Medicamento.create("ZZDEL-" + UUID.randomUUID(), estadoId, null);
        repository.save(m);

        repository.delete(m.getId());

        assertTrue(repository.findByUuid(m.getId()).isEmpty());
    }
}
