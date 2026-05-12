package itesm.medsync.infrastructure.persistence.article;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.shared.model.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityGraph;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class ArticleRepositoryImpl
        implements ArticleRepository, PanacheRepositoryBase<ArticleEntity, UUID> {

    private static final String GRAPH_WITH_TAGS = "Article.withTags";
    private static final String FETCH_GRAPH_HINT = "jakarta.persistence.fetchgraph";

    @Override
    public Article save(Article article) {
        Optional<ArticleEntity> existing = findByIdOptional(article.getId());
        if (existing.isPresent()) {
            ArticleEntity managed = existing.get();
            ArticlePersistenceMapper.copyScalars(article, managed);
            reconcileTags(managed, article.getTags());
            getEntityManager().flush();
            return ArticlePersistenceMapper.toDomain(managed);
        }
        ArticleEntity fresh = ArticlePersistenceMapper.toEntity(article);
        persist(fresh);
        flush();
        return ArticlePersistenceMapper.toDomain(fresh);
    }

    private void reconcileTags(ArticleEntity managed, List<ArticleTag> desiredTags) {
        Map<UUID, ArticleTag> desiredById = new HashMap<>();
        for (ArticleTag tag : desiredTags) {
            desiredById.put(tag.getId(), tag);
        }
        managed.getTags().removeIf(existing -> !desiredById.containsKey(existing.getId()));
        for (ArticleTag desired : desiredTags) {
            boolean alreadyManaged = managed.getTags().stream()
                    .anyMatch(t -> t.getId().equals(desired.getId()));
            if (!alreadyManaged) {
                ArticleTagEntity newTag = ArticlePersistenceMapper.toTagEntity(desired);
                newTag.setArticulo(managed);
                managed.getTags().add(newTag);
            }
        }
    }

    @Override
    public Optional<Article> findByUuid(UUID id) {
        EntityGraph<?> graph = getEntityManager().getEntityGraph(GRAPH_WITH_TAGS);
        List<ArticleEntity> results = getEntityManager()
                .createQuery("SELECT a FROM ArticleEntity a WHERE a.id = :id", ArticleEntity.class)
                .setParameter("id", id)
                .setHint(FETCH_GRAPH_HINT, graph)
                .getResultList();
        return results.stream().findFirst().map(ArticlePersistenceMapper::toDomain);
    }

    @Override
    public Page<Article> listArticles(int page, int size) {
        long total = getEntityManager()
                .createQuery("SELECT COUNT(a) FROM ArticleEntity a", Long.class)
                .getSingleResult();
        if (total == 0) {
            return Page.of(List.of(), 0L, page, size);
        }

        List<UUID> ids = getEntityManager()
                .createQuery("SELECT a.id FROM ArticleEntity a ORDER BY a.id", UUID.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
        if (ids.isEmpty()) {
            return Page.of(List.of(), total, page, size);
        }

        List<Article> items = loadArticlesByIds(ids);
        return Page.of(items, total, page, size);
    }

    @Override
    public boolean existsByDoi(String doi) {
        Long count = getEntityManager()
                .createQuery("SELECT COUNT(a) FROM ArticleEntity a WHERE a.doi = :doi", Long.class)
                .setParameter("doi", doi)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsByUrl(String url) {
        if (url == null || url.isBlank()) return false;
        Long count = getEntityManager()
                .createQuery("SELECT COUNT(a) FROM ArticleEntity a WHERE a.url = :url", Long.class)
                .setParameter("url", url)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public List<Article> findMatchingArticlesForPaciente(UUID pacienteId, int limit) {
        List<UUID> ids = getEntityManager().createQuery(
                "SELECT DISTINCT a.id FROM ArticleEntity a JOIN a.tags at " +
                "WHERE EXISTS (" +
                "  SELECT 1 FROM itesm.medsync.infrastructure.persistence.pacientecontexto.PacienteContextoEntity pc " +
                "  WHERE pc.pacienteId = :pid " +
                "    AND pc.tipo = at.tipo " +
                "    AND pc.valor = at.valor" +
                ") " +
                "ORDER BY a.id", UUID.class)
                .setParameter("pid", pacienteId)
                .setMaxResults(limit)
                .getResultList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return loadArticlesByIds(ids);
    }

    private List<Article> loadArticlesByIds(List<UUID> ids) {
        EntityGraph<?> graph = getEntityManager().getEntityGraph(GRAPH_WITH_TAGS);
        Map<UUID, ArticleEntity> byId = new HashMap<>();
        for (ArticleEntity entity : getEntityManager()
                .createQuery("SELECT a FROM ArticleEntity a WHERE a.id IN :ids", ArticleEntity.class)
                .setParameter("ids", ids)
                .setHint(FETCH_GRAPH_HINT, graph)
                .getResultList()) {
            byId.put(entity.getId(), entity);
        }
        // Preservar el orden del listado de IDs (paginación / orden estable)
        return ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(ArticlePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Page<Article> findRecentArticles(int page, int size) {
        long total = getEntityManager()
                .createQuery("SELECT COUNT(a) FROM ArticleEntity a", Long.class)
                .getSingleResult();
        if (total == 0) {
            return Page.of(List.of(), 0L, page, size);
        }

        List<UUID> ids = getEntityManager()
                .createQuery(
                        "SELECT a.id FROM ArticleEntity a ORDER BY a.updatedAt DESC, a.id ASC",
                        UUID.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        if (ids.isEmpty()) {
            return Page.of(List.of(), total, page, size);
        }

        List<Article> items = loadArticlesByIds(ids);
        return Page.of(items, total, page, size);
    }
}

