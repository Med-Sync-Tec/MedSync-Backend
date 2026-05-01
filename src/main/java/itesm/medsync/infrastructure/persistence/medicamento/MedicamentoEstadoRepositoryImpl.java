package itesm.medsync.infrastructure.persistence.medicamento;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.medicamento.model.MedicamentoEstado;
import itesm.medsync.domain.medicamento.repository.MedicamentoEstadoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class MedicamentoEstadoRepositoryImpl
        implements MedicamentoEstadoRepository, PanacheRepositoryBase<MedicamentoEstadoEntity, UUID> {

    @Override
    public Optional<MedicamentoEstado> findByNombre(String nombre) {
        return find("nombre", nombre).firstResultOptional()
                .map(MedicamentoPersistenceMapper::estadoToDomain);
    }
}
