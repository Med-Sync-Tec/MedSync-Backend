package itesm.medsync.infrastructure.persistence.dashboard;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.dashboard.repository.DashboardRepository;
import itesm.medsync.infrastructure.persistence.article.ArticleEntity;
import itesm.medsync.infrastructure.persistence.article.ArticlePersistenceMapper;
import itesm.medsync.infrastructure.persistence.user.UserEntity;
import itesm.medsync.infrastructure.persistence.user.UserRepositoryImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Panache-backed implementation of {@link DashboardRepository}.
 *
 * Every query loads {@link ArticleEntity} with the {@code Article.withTags}
 * EntityGraph (per the {@code CLAUDE.md} "siempre EntityGraph — nunca LAZY
 * ni EAGER como estrategia" rule) and maps to the domain {@link Article}
 * via {@code ArticlePersistenceMapper.toDomain} before returning. This
 * keeps the back-reference {@code ArticleTagEntity.articulo} entirely
 * inside infrastructure and prevents the Jackson cycle that the previous
 * implementation triggered.
 */
@ApplicationScoped
public class DashboardRepositoryImpl
        implements DashboardRepository, PanacheRepositoryBase<ArticleEntity, UUID> {

    private static final String GRAPH_WITH_TAGS = "Article.withTags";
    private static final String FETCH_GRAPH_HINT = "jakarta.persistence.fetchgraph";

    private final UserRepositoryImpl userRepository;
    private final EntityManager entityManager;

    @Inject
    public DashboardRepositoryImpl(UserRepositoryImpl userRepository, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    @Override
    public List<Article> findNovedades48h() {
        LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minusHours(48);
        return runJpqlWithGraph(
                "SELECT a FROM ArticleEntity a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC",
                params(p -> p.put("since", since)));
    }

    @Override
    public List<Article> findNoLeidos(UUID userId) {
        return runJpqlWithGraph(
                "SELECT a FROM ArticleEntity a WHERE a.id NOT IN ("
                        + "  SELECT ual.articulo.id FROM UsuarioArticuloLeidoEntity ual "
                        + "  WHERE ual.usuario.id = :uid"
                        + ") ORDER BY a.createdAt DESC",
                params(p -> p.put("uid", userId)));
    }

    @Override
    public List<Article> findPorEspecialidad(UUID userId) {
        UserEntity user = userRepository.findById(userId);
        if (user == null || user.getEspecialidadId() == null) {
            return List.of();
        }
        return runJpqlWithGraph(
                "SELECT a FROM ArticleEntity a WHERE a.especialidadId = :eid ORDER BY a.createdAt DESC",
                params(p -> p.put("eid", user.getEspecialidadId())));
    }

    @Override
    public List<Article> findAltaEvidencia() {
        return runJpqlWithGraph(
                "SELECT a FROM ArticleEntity a WHERE a.tipoPublicacion = :tp ORDER BY a.createdAt DESC",
                params(p -> p.put("tp", "Journal Article")));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<Article> runJpqlWithGraph(String jpql, java.util.Map<String, Object> params) {
        EntityGraph<?> graph = entityManager.getEntityGraph(GRAPH_WITH_TAGS);
        var query = entityManager.createQuery(jpql, ArticleEntity.class)
                .setHint(FETCH_GRAPH_HINT, graph);
        params.forEach(query::setParameter);
        return query.getResultList().stream()
                .map(ArticlePersistenceMapper::toDomain)
                .toList();
    }

    private static java.util.Map<String, Object> params(java.util.function.Consumer<java.util.Map<String, Object>> filler) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        filler.accept(map);
        return map;
    }
}
