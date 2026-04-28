package itesm.medsync.infrastructure.persistence.pacientecontexto;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.pacientecontexto.repository.PacienteContextoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class PacienteContextoRepositoryImpl
        implements PacienteContextoRepository, PanacheRepositoryBase<PacienteContextoEntity, UUID> {

    @Override
    public PacienteContexto save(PacienteContexto contexto) {
        PacienteContextoEntity entity = PacienteContextoPersistenceMapper.toEntity(contexto);
        persist(entity);
        flush();
        return PacienteContextoPersistenceMapper.toDomain(entity);
    }

    @Override
    public Optional<PacienteContexto> findByUuid(UUID id) {
        return findByIdOptional(id)
                .map(PacienteContextoPersistenceMapper::toDomain);
    }

    @Override
    public List<PacienteContexto> findByPacienteId(UUID pacienteId) {
        return list("pacienteId = ?1 order by createdAt desc", pacienteId).stream()
                .map(PacienteContextoPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void removeById(UUID id) {
        findByIdOptional(id).ifPresent(entity -> {
            delete(entity);
            flush();
        });
    }
}
