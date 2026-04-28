package itesm.medsync.interfaces.rest.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;

import java.util.List;

public final class ArticleRestMapper {

    private ArticleRestMapper() {
    }

    public static ArticleResponse toResponse(Article article) {
        List<ArticleTagResponse> tags = article.getTags().stream()
                .map(ArticleRestMapper::toTagResponse)
                .toList();
        return new ArticleResponse(
                article.getId(),
                article.getTitulo(),
                article.getAutores(),
                article.getRevista(),
                article.getAnioPub(),
                article.getMesPub(),
                article.getDoi(),
                article.getAbstractText(),
                article.getKeywords(),
                article.getTipoPublicacion(),
                article.getUrl(),
                tags,
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }

    public static ArticleTagResponse toTagResponse(ArticleTag tag) {
        return new ArticleTagResponse(
                tag.getId(),
                tag.getTipo().name().toLowerCase(),
                tag.getValor(),
                tag.getCreatedAt()
        );
    }
}
