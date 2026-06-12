package itesm.medsync.interfaces.rest.pacientecontexto;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
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
class PacienteContextoResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    PatientRepository patientRepository;

    @InjectMock
    AuthenticatedUserContext userContext;

    private UUID patientId;

    @BeforeEach
    void setupPaciente() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId)
                .orElseThrow(() -> new AssertionError("DOCTOR role debe existir (seed V3)"));
        User medico = userRepository.save(
                User.create("IT Medico", "it-ctx-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        Patient p = patientRepository.save(
                Patient.create("EXP-IT-CTX-" + UUID.randomUUID(),
                        "Paciente IT", LocalDate.of(1990, Month.JANUARY, 1), "F", medico.getId()));
        patientId = p.getId();

        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(medico, "DOCTOR"));
    }

    private Map<String, Object> validPayload(String tipo, String valor) {
        Map<String, Object> m = new HashMap<>();
        m.put("tipo", tipo);
        m.put("valor", valor);
        return m;
    }

    @Test
    @DisplayName("POST válido devuelve 201 con body, Location y campos normalizados")
    void addOk() {
        given()
                .contentType(ContentType.JSON)
                .body(validPayload("enfermedad", "Hipertensión"))
                .when().post("/api/patients/" + patientId + "/contextos")
                .then()
                .statusCode(201)
                .header("Location", containsString("/contextos/"))
                .body("id", not(emptyOrNullString()))
                .body("pacienteId", equalTo(patientId.toString()))
                .body("tipo", equalTo("enfermedad"))
                .body("valor", equalTo("Hipertensión"))
                .body("createdAt", not(emptyOrNullString()));
    }

    @Test
    @DisplayName("POST con tipo inválido devuelve 400")
    void addInvalidTipo() {
        given()
                .contentType(ContentType.JSON)
                .body(validPayload("otro", "x"))
                .when().post("/api/patients/" + patientId + "/contextos")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("POST con valor blank devuelve 400")
    void addBlankValor() {
        given()
                .contentType(ContentType.JSON)
                .body(validPayload("enfermedad", "   "))
                .when().post("/api/patients/" + patientId + "/contextos")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("POST a paciente inexistente devuelve 404")
    void addPatientNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body(validPayload("enfermedad", "x"))
                .when().post("/api/patients/" + UUID.randomUUID() + "/contextos")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("GET lista los contextos del paciente")
    void listByPaciente() {
        given().contentType(ContentType.JSON)
                .body(validPayload("enfermedad", "HTA"))
                .when().post("/api/patients/" + patientId + "/contextos")
                .then().statusCode(201);

        given().contentType(ContentType.JSON)
                .body(validPayload("medicamento", "Losartán"))
                .when().post("/api/patients/" + patientId + "/contextos")
                .then().statusCode(201);

        given().when().get("/api/patients/" + patientId + "/contextos")
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("valor", hasItems("HTA", "Losartán"));
    }

    @Test
    @DisplayName("GET de paciente inexistente devuelve 404")
    void listPatientNotFound() {
        given().when().get("/api/patients/" + UUID.randomUUID() + "/contextos")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("DELETE de contexto existente devuelve 204; luego GET no lo muestra")
    void deleteFlow() {
        String contextoId = given().contentType(ContentType.JSON)
                .body(validPayload("sintoma", "Mareo"))
                .when().post("/api/patients/" + patientId + "/contextos")
                .then().statusCode(201)
                .extract().path("id");

        given().when().delete("/api/patients/" + patientId + "/contextos/" + contextoId)
                .then().statusCode(204);

        given().when().get("/api/patients/" + patientId + "/contextos")
                .then().statusCode(200)
                .body("findAll { it.id == '" + contextoId + "' }", hasSize(0));
    }

    @Test
    @DisplayName("DELETE de contexto inexistente devuelve 404")
    void deleteNotFound() {
        given().when().delete("/api/patients/" + patientId + "/contextos/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    @DisplayName("DELETE de contexto que pertenece a otro paciente devuelve 404 (no enumeration)")
    void deleteCrossPatient() {
        String contextoId = given().contentType(ContentType.JSON)
                .body(validPayload("sintoma", "Mareo"))
                .when().post("/api/patients/" + patientId + "/contextos")
                .then().statusCode(201)
                .extract().path("id");

        UUID otherPatient = UUID.randomUUID();
        given().when().delete("/api/patients/" + otherPatient + "/contextos/" + contextoId)
                .then().statusCode(404);

        given().when().get("/api/patients/" + patientId + "/contextos")
                .then().statusCode(200)
                .body("findAll { it.id == '" + contextoId + "' }", hasSize(1));
    }

    @Test
    @DisplayName("Sin autenticación → 401 en todos los endpoints")
    void requiresAuth() {
        when(userContext.getCurrentUser()).thenReturn(null);

        given().when().get("/api/patients/" + patientId + "/contextos")
                .then().statusCode(401);

        given().contentType(ContentType.JSON)
                .body(validPayload("enfermedad", "x"))
                .when().post("/api/patients/" + patientId + "/contextos")
                .then().statusCode(401);

        given().when().delete("/api/patients/" + patientId + "/contextos/" + UUID.randomUUID())
                .then().statusCode(401);
    }
}
