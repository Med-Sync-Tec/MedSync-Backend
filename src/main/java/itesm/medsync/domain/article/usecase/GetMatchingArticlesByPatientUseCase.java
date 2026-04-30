package itesm.medsync.domain.article.usecase;

import itesm.medsync.domain.article.model.Article;

import java.util.List;
import java.util.UUID;

public interface GetMatchingArticlesByPatientUseCase {

    List<Article> execute(UUID patientId, int limit);
}
