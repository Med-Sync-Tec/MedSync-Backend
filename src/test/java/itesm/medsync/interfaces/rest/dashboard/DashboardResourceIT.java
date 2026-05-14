package itesm.medsync.interfaces.rest.dashboard;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.domain.user.repository.RoleRepository;
import itesm.medsync.domain.user.repository.UserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for the {@code GET /api/v1/dashboard/kpis} endpoint.
 *
 * The original implementation leaked {@code ArticleEntity} directly into
 * the REST envelope, and Jackson followed the bidirectional
 * {@code ArticleEntity ↔ ArticleTagEntity} back-reference until it hit the
 * 1000-depth ceiling — every analyze-with-AI invocation tripped this in
 * production. These tests pin down two invariants:
 *  - the response is valid JSON regardless of how many tags an article has;
 *  - the shape matches {@code ArticleResponse}, which is what the frontend
 *    already binds to.
 */
@QuarkusTest
class DashboardResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    SpecialtyRepository specialtyRepository;

    @Inject
    ArticleRepository articleRepository;

    @InjectMock
    AuthenticatedUserContext userContext;

    private UUID userId;

    @BeforeEach
    void setUp() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        UUID cardiologiaId = specialtyRepository.findBySlug("cardiologia")
                .map(Specialty::getId).orElseThrow();
        User medico = userRepository.save(
                User.create("Dash IT", "dash-" + UUID.randomUUID() + "@tec.mx",
                        cardiologiaId, doctorRoleId));
        userId = medico.getId();
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(medico, "DOCTOR"));
    }

    private void seedTaggedArticle(String doi) {
        Article article = Article.create(
                "Tratamiento de IC", "Autor", "JAMA", 2024, "Mar",
                doi, "abstract", "ic", "Journal Article",
                "https://doi.org/" + doi);
        UUID cardio = specialtyRepository.findBySlug("cardiologia")
                .map(Specialty::getId).orElseThrow();
        articleRepository.save(article.withAiAnalysis(cardio, List.of(
                ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión arterial"),
                ArticleTag.create(TipoClinico.MEDICAMENTO, "Losartán"))));
    }

    @Test
    @DisplayName("200: dashboard returns valid JSON with tagged articles — no Jackson recursion")
    void noRecursionOnTaggedArticles() {
        seedTaggedArticle("IT-DASH-" + UUID.randomUUID());

        given().when().get("/api/v1/dashboard/kpis")
                .then().statusCode(200)
                .body("novedades_48h", instanceOf(java.util.List.class))
                .body("no_leidos", instanceOf(java.util.List.class))
                .body("por_especialidad", instanceOf(java.util.List.class))
                .body("alta_evidencia", instanceOf(java.util.List.class));
    }

    @Test
    @DisplayName("Article tags in the response use the ArticleTagResponse shape (no back-reference to articulo)")
    void tagShapeMatchesArticleResponse() {
        seedTaggedArticle("IT-DASH-SHAPE-" + UUID.randomUUID());

        // alta_evidencia is filtered by tipoPublicacion="Journal Article", which our
        // seed sets, so at least one article must surface here. The tag entries should
        // carry only {id, tipo, valor, createdAt} — never an embedded `articulo` field
        // that would indicate the JPA entity leaked through.
        given().when().get("/api/v1/dashboard/kpis")
                .then().statusCode(200)
                .body("alta_evidencia.size()", greaterThan(0))
                .body("alta_evidencia[0].tags[0]", hasKey("tipo"))
                .body("alta_evidencia[0].tags[0]", hasKey("valor"))
                .body("alta_evidencia[0].tags[0]", not(hasKey("articulo")));
    }

    @Test
    @DisplayName("200 con buckets vacíos cuando no hay artículos sembrados para el usuario")
    void emptyBucketsAreFineToo() {
        // Sin seed: no_leidos puede traer artículos legacy del PubMed sync,
        // pero el endpoint debe responder 200 con un payload bien formado.
        given().when().get("/api/v1/dashboard/kpis")
                .then().statusCode(200)
                .body("$", hasKey("novedades_48h"))
                .body("$", hasKey("no_leidos"))
                .body("$", hasKey("por_especialidad"))
                .body("$", hasKey("alta_evidencia"));
    }
}
