package itesm.medsync.domain.shared.model;

import itesm.medsync.domain.vocabulary.model.Vocabulary;

/**
 * Input to {@code AiAnalysisGateway.analyzeConsultaText} — feature 4.
 *
 * Shipped here in feature 3 so the shared gateway port can be defined once;
 * feature 4 will fill in the calling code.
 */
public record ConsultaAnalysisRequest(String consultaText, Vocabulary vocabulary) {
}
