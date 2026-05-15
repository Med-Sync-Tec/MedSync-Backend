package itesm.medsync.application.consultaaianalysis;

import itesm.medsync.domain.consultaaianalysis.exception.InvalidConsultaDataException;
import itesm.medsync.domain.consultaaianalysis.exception.UserHasNoSpecialtyException;
import itesm.medsync.domain.consultaaianalysis.model.ConsultaAnalysis;
import itesm.medsync.domain.consultaaianalysis.model.VocabularyStatus;
import itesm.medsync.domain.consultaaianalysis.usecase.AnalyzeConsultaWithAiUseCase;
import itesm.medsync.domain.hospital.exception.ConsultaNotFoundException;
import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.repository.HospitalGateway;
import itesm.medsync.domain.shared.model.ConsultaAnalysisRequest;
import itesm.medsync.domain.shared.model.ConsultaAnalysisResult;
import itesm.medsync.domain.shared.repository.AiAnalysisGateway;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.repository.VocabularyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Orchestrates the read-only consulta-analysis flow.
 *
 * Not {@code @Transactional} — the service makes no DB writes. The bulk
 * patient-context endpoint persists the doctor-curated subset of these
 * suggestions in its own transaction.
 *
 * Steps: assert caller has a specialty → load consulta (404) → join SOAP
 * (400 if all blank) → load vocabulary → if vocabulary is empty, return
 * {@link VocabularyStatus#EMPTY} without calling the LLM (cost saver and
 * clearer UX than letting the model hallucinate without constraints) →
 * otherwise call {@link AiAnalysisGateway#analyzeConsultaText} and wrap
 * the result.
 */
@ApplicationScoped
public class AnalyzeConsultaWithAiService implements AnalyzeConsultaWithAiUseCase {

    private static final Logger LOG = Logger.getLogger(AnalyzeConsultaWithAiService.class);

    private final HospitalGateway hospitalGateway;
    private final VocabularyRepository vocabularyRepository;
    private final AiAnalysisGateway aiGateway;

    @Inject
    public AnalyzeConsultaWithAiService(HospitalGateway hospitalGateway,
                                        VocabularyRepository vocabularyRepository,
                                        AiAnalysisGateway aiGateway) {
        this.hospitalGateway = hospitalGateway;
        this.vocabularyRepository = vocabularyRepository;
        this.aiGateway = aiGateway;
    }

    @Override
    public ConsultaAnalysis execute(String consultaId, User caller) {
        if (caller.getEspecialidadId() == null) {
            throw new UserHasNoSpecialtyException(
                    "Authenticated user has no especialidadId — set a specialty before invoking AI analysis");
        }

        Consulta consulta = hospitalGateway.findConsultaById(consultaId)
                .orElseThrow(() -> new ConsultaNotFoundException("Consulta not found: " + consultaId));

        ConsultaSoapJoiner.JoinedConsulta joined = ConsultaSoapJoiner.join(consulta);
        if (joined.allBlank()) {
            throw new InvalidConsultaDataException(
                    "Consulta " + consultaId + " has no analyzable text — all SOAP fields are blank");
        }
        if (joined.wasTruncated()) {
            LOG.warnf("ai.consulta.truncated consultaId=%s originalLength=%d truncatedAt=%d",
                    consultaId, joined.originalLength(), joined.text().length());
        }

        Vocabulary vocabulary = vocabularyRepository.getVocabularyFor(caller.getEspecialidadId());
        if (vocabulary.totalTerms() == 0) {
            LOG.infof("ai.consulta.empty_vocab consultaId=%s especialidadId=%s — short-circuited (no LLM call)",
                    consultaId, caller.getEspecialidadId());
            return new ConsultaAnalysis(
                    consultaId, caller.getEspecialidadId(), vocabulary.getEspecialidadSlug(),
                    VocabularyStatus.EMPTY, List.of(),
                    "", 0, 0);
        }

        ConsultaAnalysisResult result = aiGateway.analyzeConsultaText(
                new ConsultaAnalysisRequest(joined.text(), vocabulary));

        LOG.infof("ai.consulta.analyzed consultaId=%s especialidadId=%s tags=%d prompt_tokens=%d completion_tokens=%d",
                consultaId, caller.getEspecialidadId(),
                result.tags().size(), result.promptTokens(), result.completionTokens());

        return new ConsultaAnalysis(
                consultaId, caller.getEspecialidadId(), vocabulary.getEspecialidadSlug(),
                VocabularyStatus.POPULATED, result.tags(),
                result.modelUsed(), result.promptTokens(), result.completionTokens());
    }
}
