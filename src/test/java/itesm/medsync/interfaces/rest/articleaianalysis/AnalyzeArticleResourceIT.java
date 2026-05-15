package itesm.medsync.interfaces.rest.articleaianalysis;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisException;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisTimeoutException;
import itesm.medsync.domain.shared.model.ArticleAnalysisRequest;
import itesm.medsync.domain.shared.model.ArticleAnalysisResult;
import itesm.medsync.domain.shared.model.ExtractedTag;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.shared.repository.AiAnalysisGateway;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class AnalyzeArticleResourceIT {

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

    @InjectMock
    AiAnalysisGateway aiGateway;

    private UUID cardiologiaId;
    private User medico;

    @BeforeEach
    void setUp() {
        cardiologiaId = specialtyRepository.findBySlug("cardiologia")
                .map(Specialty::getId).orElseThrow();
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        medico = userRepository.save(
                User.create("Ana", "ai-it-" + UUID.randomUUID() + "@tec.mx",
                        cardiologiaId, doctorRoleId));
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(medico, "DOCTOR"));
    }

    private Map<String, Object> articlePayload(String doi, boolean withAbstract) {
        Map<String, Object> m = new HashMap<>();
        m.put("titulo", "Tratamiento de insuficiencia cardíaca");
        m.put("doi", doi);
        m.put("anioPub", 2024);
        if (withAbstract) {
            m.put("abstractText", "Resumen de IC tratada con losartán.");
        }
        m.put("keywords", "ic, losartan");
        return m;
    }

    private String createArticleViaRest(boolean withAbstract) {
        return given().contentType(ContentType.JSON)
                .body(articlePayload("IT-AI-" + UUID.randomUUID(), withAbstract))
                .when().post("/api/articles")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    @DisplayName("200: happy path con abstract → tags persistidos, hadAbstract=true, body completo")
    void happyPathWithAbstract() {
        String articleId = createArticleViaRest(true);
        when(aiGateway.analyzeArticle(any(ArticleAnalysisRequest.class))).thenReturn(
                new ArticleAnalysisResult(
                        cardiologiaId,
                        List.of(new ExtractedTag(TipoClinico.ENFERMEDAD, "Hipertensión arterial")),
                        "llama-3.3-70b-versatile", 500, 80, true));

        given().when().post("/api/articles/" + articleId + "/analyze")
                .then().statusCode(200)
                .body("articleId", equalTo(articleId))
                .body("especialidadId", equalTo(cardiologiaId.toString()))
                .body("especialidadNombre", equalTo("Cardiología"))
                .body("modelUsed", equalTo("llama-3.3-70b-versatile"))
                .body("promptTokens", equalTo(500))
                .body("completionTokens", equalTo(80))
                .body("hadAbstract", equalTo(true))
                .body("tags", hasSize(1))
                .body("tags[0].tipo", equalTo("ENFERMEDAD"))
                .body("tags[0].valor", equalTo("Hipertensión arterial"));

        given().when().get("/api/articles/" + articleId)
                .then().statusCode(200)
                .body("tags", hasSize(1))
                .body("especialidadId", equalTo(cardiologiaId.toString()));
    }

    @Test
    @DisplayName("200: artículo sin abstract → hadAbstract=false y tags no vacíos")
    void happyPathNoAbstract() {
        String articleId = createArticleViaRest(false);
        when(aiGateway.analyzeArticle(any(ArticleAnalysisRequest.class))).thenReturn(
                new ArticleAnalysisResult(
                        cardiologiaId,
                        List.of(new ExtractedTag(TipoClinico.ENFERMEDAD, "Hipertensión arterial")),
                        "llama-3.3-70b-versatile", 200, 30, false));

        given().when().post("/api/articles/" + articleId + "/analyze")
                .then().statusCode(200)
                .body("hadAbstract", equalTo(false))
                .body("tags", hasSize(greaterThan(0)));
    }

    @Test
    @DisplayName("404: id desconocido")
    void notFound() {
        given().when().post("/api/articles/" + UUID.randomUUID() + "/analyze")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("502: gateway lanza AiAnalysisException")
    void badGateway() {
        String articleId = createArticleViaRest(true);
        when(aiGateway.analyzeArticle(any(ArticleAnalysisRequest.class)))
                .thenThrow(new AiAnalysisException("Groq classify returned HTTP 500"));

        given().when().post("/api/articles/" + articleId + "/analyze")
                .then().statusCode(502)
                .body("message", containsString("AI analysis failed"));
    }

    @Test
    @DisplayName("504: gateway lanza AiAnalysisTimeoutException")
    void gatewayTimeout() {
        String articleId = createArticleViaRest(true);
        when(aiGateway.analyzeArticle(any(ArticleAnalysisRequest.class)))
                .thenThrow(new AiAnalysisTimeoutException("slow upstream"));

        given().when().post("/api/articles/" + articleId + "/analyze")
                .then().statusCode(504)
                .body("message", containsString("timeout"));
    }

    @Test
    @DisplayName("401: sin autenticación")
    void unauthorized() {
        when(userContext.getCurrentUser()).thenReturn(null);
        given().when().post("/api/articles/" + UUID.randomUUID() + "/analyze")
                .then().statusCode(401);
    }
}
