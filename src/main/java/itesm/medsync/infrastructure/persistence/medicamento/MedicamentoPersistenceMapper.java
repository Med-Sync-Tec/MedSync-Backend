package itesm.medsync.infrastructure.persistence.medicamento;

import itesm.medsync.domain.medicamento.model.Medicamento;
import itesm.medsync.domain.medicamento.model.MedicamentoEstado;
import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;

public final class MedicamentoPersistenceMapper {

    private MedicamentoPersistenceMapper() {}

    public static MedicamentoWithEstado toDomain(MedicamentoEntity entity) {
        MedicamentoEstado estado = new MedicamentoEstado(
                entity.getEstado().getId(),
                entity.getEstado().getNombre(),
                entity.getEstado().getDescripcion()
        );
        Medicamento medicamento = new Medicamento(
                entity.getId(),
                entity.getNombre(),
                entity.getEstado().getId(),
                entity.getDescripcion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
        return new MedicamentoWithEstado(medicamento, estado);
    }

    public static MedicamentoEstado estadoToDomain(MedicamentoEstadoEntity entity) {
        return new MedicamentoEstado(entity.getId(), entity.getNombre(), entity.getDescripcion());
    }

    public static MedicamentoEntity toEntity(Medicamento medicamento, MedicamentoEstadoEntity estadoEntity) {
        MedicamentoEntity entity = new MedicamentoEntity();
        entity.setId(medicamento.getId());
        entity.setNombre(medicamento.getNombre());
        entity.setEstado(estadoEntity);
        entity.setDescripcion(medicamento.getDescripcion());
        return entity;
    }
}
