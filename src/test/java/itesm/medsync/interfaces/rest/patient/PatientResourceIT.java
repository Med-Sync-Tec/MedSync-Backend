package itesm.medsync.interfaces.rest.patient;

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

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class PatientResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @InjectMock
    AuthenticatedUserContext userContext;

    private User testMedico;

    @BeforeEach
    void setupTestMedico() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId)
                .orElseThrow(() -> new AssertionError("DOCTOR role debe existir (seed V3)"));
        testMedico = userRepository.save(
                User.create("Test Medico", "test-medico-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(testMedico, "DOCTOR"));
    }

    private Map<String, Object> validPayload(String expediente) {
        Map<String, Object> m = new HashMap<>();
        m.put("expedienteExternoId", expediente);
        m.put("nombre", "Juan Perez");
        m.put("fechaNacimiento", "1990-05-20");
        m.put("genero", "M");
        return m;
    }

    @Test
    @DisplayName("POST válido devuelve 201 con body, Location y medicoId del contexto")
    void createValid() {
        String expediente = "IT-" + UUID.randomUUID();
        given()
                .contentType(ContentType.JSON)
                .body(validPayload(expediente))
                .when().post("/api/patients")
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/patients/"))
                .body("id", not(emptyOrNullString()))
                .body("expedienteExternoId", equalTo(expediente))
                .body("nombre", equalTo("Juan Perez"))
                .body("medicoId", equalTo(testMedico.getId().toString()))
                .body("activo", equalTo(true))
                .body("createdAt", not(emptyOrNullString()));
    }

    @Test
    @DisplayName("POST sin usuario autenticado devuelve 401")
    void createUnauthenticated() {
        when(userContext.getCurrentUser()).thenReturn(null);
        given()
                .contentType(ContentType.JSON)
                .body(validPayload("IT-UNAUTH-" + UUID.randomUUID()))
                .when().post("/api/patients")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("POST con nombre blank devuelve 400")
    void createBlankNombre() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("expedienteExternoId", "IT-" + UUID.randomUUID());
        payload.put("nombre", "   ");
        payload.put("fechaNacimiento", "1990-01-01");
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/api/patients")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("POST con fechaNacimiento futura devuelve 400")
    void createFutureFecha() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("expedienteExternoId", "IT-" + UUID.randomUUID());
        payload.put("nombre", "Juan");
        payload.put("fechaNacimiento", LocalDate.of(2099, Month.DECEMBER, 31).toString());
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/api/patients")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("POST sin genero (opcional) devuelve 201")
    void createWithoutGenero() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("expedienteExternoId", "IT-NOGEN-" + UUID.randomUUID());
        payload.put("nombre", "Ana");
        payload.put("fechaNacimiento", "1985-03-03");
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/api/patients")
                .then().statusCode(201)
                .body("genero", nullValue());
    }

    @Test
    @DisplayName("POST con expediente duplicado devuelve 409")
    void createDuplicate() {
        String expediente = "IT-DUP-" + UUID.randomUUID();
        Map<String, Object> payload = validPayload(expediente);

        given().contentType(ContentType.JSON).body(payload)
                .when().post("/api/patients").then().statusCode(201);

        given().contentType(ContentType.JSON).body(payload)
                .when().post("/api/patients").then().statusCode(409);
    }

    @Test
    @DisplayName("POST con expediente de un paciente soft-deleted devuelve 409")
    void createDuplicateAfterSoftDelete() {
        String expediente = "IT-SD-" + UUID.randomUUID();
        String id = given().contentType(ContentType.JSON)
                .body(validPayload(expediente))
                .when().post("/api/patients")
                .then().statusCode(201)
                .extract().path("id");

        given().when().delete("/api/patients/" + id).then().statusCode(204);

        given().contentType(ContentType.JSON)
                .body(validPayload(expediente))
                .when().post("/api/patients")
                .then().statusCode(409);
    }

    @Test
    @DisplayName("GET /api/patients devuelve 200 con lista")
    void listActive() {
        given().when().get("/api/patients")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    @DisplayName("GET /api/patients/{uuid-inexistente} devuelve 404")
    void getByIdNotFound() {
        given().when().get("/api/patients/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    @DisplayName("GET /api/patients/{id-inválido} devuelve 400 o 404")
    void getByIdInvalidUuid() {
        given().when().get("/api/patients/not-a-uuid")
                .then().statusCode(anyOf(is(400), is(404)));
    }

    @Test
    @DisplayName("DELETE existente devuelve 204; GET luego no lo muestra")
    void softDeleteFlow() {
        String expediente = "IT-DEL-" + UUID.randomUUID();
        String id = given().contentType(ContentType.JSON)
                .body(validPayload(expediente))
                .when().post("/api/patients")
                .then().statusCode(201)
                .extract().path("id");

        given().when().delete("/api/patients/" + id).then().statusCode(204);

        given().when().get("/api/patients/" + id).then().statusCode(404);

        given().when().get("/api/patients")
                .then().statusCode(200)
                .body("findAll { it.id == '" + id + "' }", hasSize(0));
    }

    @Test
    @DisplayName("DELETE inexistente devuelve 404")
    void deleteNotFound() {
        given().when().delete("/api/patients/" + UUID.randomUUID())
                .then().statusCode(404);
    }
}
