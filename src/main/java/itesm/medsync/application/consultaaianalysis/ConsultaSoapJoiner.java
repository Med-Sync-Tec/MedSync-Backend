package itesm.medsync.application.consultaaianalysis;

import itesm.medsync.domain.hospital.model.Consulta;

/**
 * Joins a {@link Consulta}'s SOAP and adjunct fields into a single text blob
 * for the LLM, replacing null/blank fields with {@code "(no registrado)"} so
 * the model still sees the consulta's structural shape (e.g. "diagnóstico
 * present, plan absent").
 *
 * Package-private — used by {@link AnalyzeConsultaWithAiService}. Static
 * helper because there is no state and no DI dependency; the return type
 * carries enough signal for the caller to throw or log appropriately.
 *
 * Cap at 50,000 characters guards against runaway inputs (e.g. a hospital
 * row with a copy-pasted PDF in {@code subjetivo}). Real consultas are
 * 1–5 KB so the cap is effectively a safety net, not a limit doctors hit.
 */
final class ConsultaSoapJoiner {

    static final int MAX_LENGTH = 50_000;
    static final String PLACEHOLDER = "(no registrado)";

    private ConsultaSoapJoiner() {
    }

    /**
     * Aggregates the join result. {@code allBlank} fires when every section
     * collapsed to the placeholder — the caller should throw
     * {@code InvalidConsultaDataException} in that case rather than send a
     * structurally empty prompt to the LLM.
     */
    record JoinedConsulta(String text, boolean allBlank, boolean wasTruncated, int originalLength) {
    }

    static JoinedConsulta join(Consulta consulta) {
        String[] labels = {
                "[MOTIVO DE CONSULTA]",
                "[SUBJETIVO]",
                "[OBJETIVO]",
                "[EVALUACIÓN]",
                "[PLAN]",
                "[PRESCRIPCIÓN]",
                "[DIAGNÓSTICO]"
        };
        String[] rawValues = {
                consulta.getMotivoConsulta(),
                consulta.getSubjetivo(),
                consulta.getObjetivo(),
                consulta.getEvaluacion(),
                consulta.getPlan(),
                consulta.getPrescripcion(),
                consulta.getDiagnostico()
        };

        boolean allBlank = true;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < labels.length; i++) {
            String value = rawValues[i];
            boolean isBlank = value == null || value.isBlank();
            if (!isBlank) {
                allBlank = false;
            }
            sb.append(labels[i]).append("\n")
                    .append(isBlank ? PLACEHOLDER : value.trim())
                    .append("\n\n");
        }

        String full = sb.toString().stripTrailing();
        int originalLength = full.length();
        boolean truncated = originalLength > MAX_LENGTH;
        String text = truncated ? full.substring(0, MAX_LENGTH) : full;
        return new JoinedConsulta(text, allBlank, truncated, originalLength);
    }
}
