package itesm.medsync.domain.medicamento.model;

import java.util.List;

public record MedicamentosPage(
        List<MedicamentoWithEstado> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
