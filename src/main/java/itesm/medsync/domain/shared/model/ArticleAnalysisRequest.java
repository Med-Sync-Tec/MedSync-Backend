package itesm.medsync.domain.shared.model;

import itesm.medsync.domain.vocabulary.model.Vocabulary;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Input to {@code AiAnalysisGateway.analyzeArticle}.
 *
 * The caller (application service) is responsible for asserting that at least
 * one of {@code titulo}, {@code abstractText}, or {@code keywords} is non-blank
 * before invoking the gateway; the gateway itself trusts the contract and
 * concatenates whichever fields are non-blank into the LLM prompt.
 *
 * {@code vocabulariesById} is keyed by specialty id so the gateway can pick
 * the relevant slice once classification finishes — passing all of them keeps
 * the gateway stateless and lets the application service stay one
 * {@code VocabularyRepository} call simpler.
 */
public record ArticleAnalysisRequest(
        String titulo,
        String abstractText,
        String keywords,
        List<SpecialtyDescriptor> candidateSpecialties,
        Map<UUID, Vocabulary> vocabulariesById) {

    public boolean hasAbstract() {
        return abstractText != null && !abstractText.isBlank();
    }
}
