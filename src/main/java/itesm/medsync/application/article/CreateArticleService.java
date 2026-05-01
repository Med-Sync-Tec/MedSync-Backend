package itesm.medsync.application.article;

import itesm.medsync.domain.article.exception.DuplicateArticleException;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.article.usecase.CreateArticleUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CreateArticleService implements CreateArticleUseCase {

    private final ArticleRepository repository;

    @Inject
    public CreateArticleService(ArticleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Article execute(String titulo,
                           String autores,
                           String revista,
                           Integer anioPub,
                           String mesPub,
                           String doi,
                           String abstractText,
                           String keywords,
                           String tipoPublicacion,
                           String url) {
        if (doi != null && !doi.isBlank() && repository.existsByDoi(doi.trim())) {
            throw new DuplicateArticleException(doi.trim());
        }
        Article article = Article.create(
                titulo, autores, revista, anioPub, mesPub,
                doi, abstractText, keywords, tipoPublicacion, url);
        return repository.save(article);
    }
}
