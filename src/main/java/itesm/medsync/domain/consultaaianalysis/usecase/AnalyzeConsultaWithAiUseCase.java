package itesm.medsync.domain.consultaaianalysis.usecase;

import itesm.medsync.domain.consultaaianalysis.model.ConsultaAnalysis;
import itesm.medsync.domain.user.model.User;

/**
 * Triggers a Groq-backed extraction of clinical-context suggestions from a
 * consulta's SOAP fields, scoped to the authenticated user's specialty.
 *
 * Read-only — the use case never persists anything. The frontend reviews
 * the {@link ConsultaAnalysis} and the doctor decides which entries to
 * commit via the bulk patient-context endpoint.
 */
public interface AnalyzeConsultaWithAiUseCase {

    ConsultaAnalysis execute(String consultaId, User caller);
}
