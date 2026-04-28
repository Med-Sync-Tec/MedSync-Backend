package itesm.medsync.infrastructure.persistence.article;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.repository.ArticleRepository;
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
    public List<Article> listAllArticles() {
        EntityGraph<?> graph = getEntityManager().getEntityGraph(GRAPH_WITH_TAGS);
        return getEntityManager()
                .createQuery("SELECT DISTINCT a FROM ArticleEntity a", ArticleEntity.class)
                .setHint(FETCH_GRAPH_HINT, graph)
                .getResultList()
                .stream()
                .map(ArticlePersistenceMapper::toDomain)
                .toList();
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
    public List<Article> findMatchingArticlesForPaciente(UUID pacienteId) {
        EntityGraph<?> graph = getEntityManager().getEntityGraph(GRAPH_WITH_TAGS);
        return getEntityManager().createQuery(
                "SELECT DISTINCT a FROM ArticleEntity a " +
                "WHERE EXISTS (" +
                "  SELECT 1 FROM ArticleTagEntity at, " +
                "    itesm.medsync.infrastructure.persistence.pacientecontexto.PacienteContextoEntity pc " +
                "  WHERE at.articulo = a " +
                "    AND pc.pacienteId = :pid " +
                "    AND pc.tipo = at.tipo " +
                "    AND pc.valor = at.valor" +
                ")", ArticleEntity.class)
                .setParameter("pid", pacienteId)
                .setHint(FETCH_GRAPH_HINT, graph)
                .getResultList()
                .stream()
                .map(ArticlePersistenceMapper::toDomain)
                .toList();
    }
}
