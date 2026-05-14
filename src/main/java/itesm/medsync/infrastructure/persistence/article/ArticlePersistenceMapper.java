package itesm.medsync.infrastructure.persistence.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;

import java.util.List;

public final class ArticlePersistenceMapper {

    private ArticlePersistenceMapper() {
    }

    public static ArticleEntity toEntity(Article article) {
        ArticleEntity entity = new ArticleEntity();
        copyScalars(article, entity);
        for (ArticleTag tag : article.getTags()) {
            ArticleTagEntity tagEntity = toTagEntity(tag);
            tagEntity.setArticulo(entity);
            entity.getTags().add(tagEntity);
        }
        return entity;
    }

    public static void copyScalars(Article article, ArticleEntity entity) {
        entity.setId(article.getId());
        entity.setTitulo(article.getTitulo());
        entity.setAutores(article.getAutores());
        entity.setRevista(article.getRevista());
        entity.setAnioPub(article.getAnioPub());
        entity.setMesPub(article.getMesPub());
        entity.setDoi(article.getDoi());
        entity.setAbstractText(article.getAbstractText());
        entity.setKeywords(article.getKeywords());
        entity.setTipoPublicacion(article.getTipoPublicacion());
        entity.setUrl(article.getUrl());
        entity.setEspecialidadId(article.getEspecialidadId());
    }

    public static ArticleTagEntity toTagEntity(ArticleTag tag) {
        ArticleTagEntity entity = new ArticleTagEntity();
        entity.setId(tag.getId());
        entity.setTipo(tag.getTipo());
        entity.setValor(tag.getValor());
        return entity;
    }

    public static Article toDomain(ArticleEntity entity) {
        List<ArticleTag> tags = entity.getTags().stream()
                .map(ArticlePersistenceMapper::toTagDomain)
                .toList();
        return new Article(
                entity.getId(),
                entity.getTitulo(),
                entity.getAutores(),
                entity.getRevista(),
                entity.getAnioPub(),
                entity.getMesPub(),
                entity.getDoi(),
                entity.getAbstractText(),
                entity.getKeywords(),
                entity.getTipoPublicacion(),
                entity.getUrl(),
                entity.getEspecialidadId(),
                tags,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static ArticleTag toTagDomain(ArticleTagEntity entity) {
        return new ArticleTag(
                entity.getId(),
                entity.getTipo(),
                entity.getValor(),
                entity.getCreatedAt());
    }
}
