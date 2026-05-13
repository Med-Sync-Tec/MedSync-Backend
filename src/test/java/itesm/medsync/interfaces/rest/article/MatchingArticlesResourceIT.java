package itesm.medsync.interfaces.rest.article;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import itesm.medsync.application.security.AuthenticatedUserContext;
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
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class MatchingArticlesResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @InjectMock
    AuthenticatedUserContext userContext;

    private String patientId;

    @BeforeEach
    void setupMedicoYPaciente() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        User medico = userRepository.save(
                User.create("Match Medico", "match-it-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(medico, "DOCTOR"));

        Map<String, Object> patientPayload = new HashMap<>();
        patientPayload.put("expedienteExternoId", "MATCH-IT-" + UUID.randomUUID());
        patientPayload.put("nombre", "Match Pte");
        patientPayload.put("fechaNacimiento", "1990-05-20");
        patientPayload.put("genero", "F");

        patientId = given().contentType(ContentType.JSON).body(patientPayload)
                .when().post("/api/patients").then().statusCode(201)
                .extract().path("id");
    }

    private Map<String, Object> articulo(String doi, String titulo) {
        Map<String, Object> m = new HashMap<>();
        m.put("titulo", titulo);
        m.put("doi", doi);
        m.put("anioPub", 2024);
        return m;
    }

    private Map<String, Object> tag(String tipo, String valor) {
        Map<String, Object> m = new HashMap<>();
        m.put("tipo", tipo);
        m.put("valor", valor);
        return m;
    }

    @Test
    @DisplayName("GET matching-articles devuelve solo los artículos que cruzan con el contexto")
    void matchingFlow() {
        given().contentType(ContentType.JSON).body(tag("enfermedad", "Hipertensión"))
                .when().post("/api/patients/" + patientId + "/contextos")
                .then().statusCode(201);

        String matchingId = given().contentType(ContentType.JSON)
                .body(articulo("IT-MATCH-" + UUID.randomUUID(), "Trat HTA"))
                .when().post("/api/articles").then().statusCode(201)
                .extract().path("id");
        given().contentType(ContentType.JSON).body(tag("enfermedad", "Hipertensión"))
                .when().post("/api/articles/" + matchingId + "/tags")
                .then().statusCode(201);

        String otherId = given().contentType(ContentType.JSON)
                .body(articulo("IT-NOMATCH-" + UUID.randomUUID(), "Otro"))
                .when().post("/api/articles").then().statusCode(201)
                .extract().path("id");
        given().contentType(ContentType.JSON).body(tag("sintoma", "Mareo"))
                .when().post("/api/articles/" + otherId + "/tags")
                .then().statusCode(201);

        given().when().get("/api/patients/" + patientId + "/matching-articles")
                .then().statusCode(200)
                .body("findAll { it.id == '" + matchingId + "' }", hasSize(1))
                .body("findAll { it.id == '" + otherId + "' }", hasSize(0));
    }

    @Test
    @DisplayName("GET matching-articles a paciente inexistente devuelve 404")
    void matchingPatientNotFound() {
        given().when().get("/api/patients/" + UUID.randomUUID() + "/matching-articles")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("GET matching-articles sin contextos devuelve lista vacía")
    void matchingNoContextos() {
        given().when().get("/api/patients/" + patientId + "/matching-articles")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }
}
