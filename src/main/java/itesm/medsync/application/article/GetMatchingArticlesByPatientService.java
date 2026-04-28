package itesm.medsync.application.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.GetMatchingArticlesByPatientUseCase;
import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.patient.repository.PatientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GetMatchingArticlesByPatientService implements GetMatchingArticlesByPatientUseCase {

    private final ArticleRepository articleRepository;
    private final PatientRepository patientRepository;

    @Inject
    public GetMatchingArticlesByPatientService(ArticleRepository articleRepository,
                                               PatientRepository patientRepository) {
        this.articleRepository = articleRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    public List<Article> execute(UUID patientId) {
        patientRepository.findByUuid(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        return articleRepository.findMatchingArticlesForPaciente(patientId);
    }
}
