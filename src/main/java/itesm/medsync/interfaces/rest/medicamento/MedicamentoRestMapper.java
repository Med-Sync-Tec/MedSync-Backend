package itesm.medsync.interfaces.rest.medicamento;

import itesm.medsync.domain.medicamento.model.MedicamentoWithEstado;

public final class MedicamentoRestMapper {

    private MedicamentoRestMapper() {}

    public static MedicamentoResponse toResponse(MedicamentoWithEstado m) {
        return new MedicamentoResponse(
                m.medicamento().getId(),
                m.medicamento().getNombre(),
                m.estado().getNombre(),
                m.medicamento().getDescripcion(),
                m.medicamento().getCreatedAt(),
                m.medicamento().getUpdatedAt()
        );
    }
}
