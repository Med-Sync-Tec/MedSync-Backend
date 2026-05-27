package itesm.medsync.interfaces.rest.article;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.user.model.KnownRoles;
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
import static org.mockito.Mockito.when;

@QuarkusTest
class CooMatchingArticlesResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    ArticleRepository articleRepository;

    @Inject
    SpecialtyRepository specialtyRepository;

    @InjectMock
    AuthenticatedUserContext userContext;

    private void authAs(String roleName) {
        UUID roleId = roleRepository.findByNombre(roleName)
                .map(Role::getId)
                .orElseThrow(() -> new AssertionError(roleName + " debe existir (seed V3)"));
        User user = userRepository.save(
                User.create(roleName + " IT", roleName.toLowerCase() + "-" + UUID.randomUUID() + "@tec.mx",
                        null, roleId));
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(user, roleName));
    }

    @BeforeEach
    void authAsCoo() {
        authAs(KnownRoles.COO);
    }

    private String createMedicamento(String nombre) {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", nombre);
        m.put("descripcion", "desc");
        return given().contentType(ContentType.JSON).body(m)
                .when().post("/api/medicamentos").then().statusCode(201)
                .extract().path("id");
    }

    private String createArticle(String titulo) {
        Map<String, Object> m = new HashMap<>();
        m.put("titulo", titulo);
        m.put("doi", "COO-MATCH-" + UUID.randomUUID());
        m.put("anioPub", 2024);
        return given().contentType(ContentType.JSON).body(m)
                .when().post("/api/articles").then().statusCode(201)
                .extract().path("id");
    }

    private void attachTag(String articleId, TipoClinico tipo, String valor) {
        // El match del COO no restringe especialidad, pero withAiAnalysis exige una
        // especialidad no nula; usamos una real cualquiera del seed.
        UUID especialidadId = specialtyRepository.findBySlug("cardiologia")
                .map(Specialty::getId).orElseThrow();
        Article a = articleRepository.findByUuid(UUID.fromString(articleId)).orElseThrow();
        Article enriched = a.withAiAnalysis(especialidadId, List.of(ArticleTag.create(tipo, valor)));
        articleRepository.save(enriched);
    }

    @Test
    @DisplayName("Devuelve artículos cuyo tag MEDICAMENTO cruza (normalizado) con el catálogo")
    void matchingFlow() {
        String medName = "CooMed-" + UUID.randomUUID();
        createMedicamento(medName);

        // Match aunque difiera en mayúsculas/espacios (case-insensitive + trim)
        String matchingId = createArticle("Estudio sobre " + medName);
        attachTag(matchingId, TipoClinico.MEDICAMENTO, "  " + medName.toUpperCase() + "  ");

        // No match: tag MEDICAMENTO que no está en el catálogo
        String noMatchId = createArticle("Otro medicamento");
        attachTag(noMatchId, TipoClinico.MEDICAMENTO, "MedicamentoInexistente-" + UUID.randomUUID());

        // No match: nombre coincide pero el tipo no es MEDICAMENTO
        String wrongTipoId = createArticle("Coincide pero no es medicamento");
        attachTag(wrongTipoId, TipoClinico.ENFERMEDAD, medName);

        given().when().get("/api/coo/matching-articles")
                .then().statusCode(200)
                .body("findAll { it.id == '" + matchingId + "' }", hasSize(1))
                .body("findAll { it.id == '" + noMatchId + "' }", hasSize(0))
                .body("findAll { it.id == '" + wrongTipoId + "' }", hasSize(0));
    }

    @Test
    @DisplayName("Rol distinto de COO recibe 403")
    void forbiddenForNonCoo() {
        authAs(KnownRoles.DOCTOR);
        given().when().get("/api/coo/matching-articles")
                .then().statusCode(403);
    }

    @Test
    @DisplayName("Sin sesión recibe 401")
    void unauthorizedWhenNoUser() {
        when(userContext.getCurrentUser()).thenReturn(null);
        given().when().get("/api/coo/matching-articles")
                .then().statusCode(401);
    }
}
