package itesm.medsync.application.patient;

import itesm.medsync.domain.article.exception.ArticleNotFoundException;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import itesm.medsync.domain.patient.usecase.GetMatchingPatientsByArticleUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GetMatchingPatientsByArticleService implements GetMatchingPatientsByArticleUseCase {

    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 50;

    private final PatientRepository patientRepository;
    private final ArticleRepository articleRepository;

    @Inject
    public GetMatchingPatientsByArticleService(PatientRepository patientRepository,
                                               ArticleRepository articleRepository) {
        this.patientRepository = patientRepository;
        this.articleRepository = articleRepository;
    }

    @Override
    public List<Patient> execute(UUID articleId, int limit) {
        articleRepository.findByUuid(articleId)
                .orElseThrow(() -> new ArticleNotFoundException(articleId));
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return patientRepository.findMatchingPatientsForArticle(articleId, safeLimit);
    }
}
