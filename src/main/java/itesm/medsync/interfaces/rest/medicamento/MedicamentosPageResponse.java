package itesm.medsync.interfaces.rest.medicamento;

import java.util.List;

public record MedicamentosPageResponse(
        List<MedicamentoResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
