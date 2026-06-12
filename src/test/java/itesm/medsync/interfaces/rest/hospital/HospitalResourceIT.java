package itesm.medsync.interfaces.rest.hospital;

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

import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class HospitalResourceIT {

    @Inject
    HospitalTestFixtures fixtures;

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @InjectMock
    AuthenticatedUserContext userContext;

    @BeforeEach
    void setupTestMedico() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId)
                .orElseThrow(() -> new AssertionError("DOCTOR role debe existir (seed V3)"));
        User testMedico = userRepository.save(
                User.create("Hosp IT Medico", "hosp-it-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(testMedico, "DOCTOR"));
    }

    private Map<String, Object> patientPayload(String expedienteExternoId) {
        Map<String, Object> m = new HashMap<>();
        m.put("expedienteExternoId", expedienteExternoId);
        m.put("nombre", "Hospital IT");
        m.put("fechaNacimiento", "1990-05-20");
        m.put("genero", "M");
        return m;
    }

    private String createPatient(String expedienteExternoId) {
        return given().contentType(ContentType.JSON)
                .body(patientPayload(expedienteExternoId))
                .when().post("/api/patients")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    @DisplayName("GET /patients/{uuid}/expediente con expediente sembrado → 200 con expediente")
    void getExpedienteOk() {
        String pacExt = "PAC-EXT-" + UUID.randomUUID();
        String patientId = createPatient(pacExt);
        String expHospId = "EXP-HOSP-" + UUID.randomUUID();
        fixtures.persistExpediente(expHospId, pacExt, "DR-1");

        given().when().get("/api/patients/" + patientId + "/expediente")
                .then().statusCode(200)
                .body("id", equalTo(expHospId))
                .body("pacienteExternoId", equalTo(pacExt))
                .body("doctorResponsableId", equalTo("DR-1"))
                .body("createdAt", not(emptyOrNullString()));
    }

    @Test
    @DisplayName("GET /patients/{uuid}/expediente con paciente sin expediente → 404")
    void getExpedienteNoExpediente() {
        String pacExt = "PAC-NOEXP-" + UUID.randomUUID();
        String patientId = createPatient(pacExt);

        given().when().get("/api/patients/" + patientId + "/expediente")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("GET /patients/{uuid-inexistente}/expediente → 404")
    void getExpedientePatientNotFound() {
        given().when().get("/api/patients/" + UUID.randomUUID() + "/expediente")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("GET /patients/{uuid}/consultas con consultas sembradas → 200 lista ordenada DESC")
    void getConsultasOk() {
        String pacExt = "PAC-CONS-" + UUID.randomUUID();
        String patientId = createPatient(pacExt);
        String expHospId = "EXP-CONS-" + UUID.randomUUID();
        fixtures.persistExpediente(expHospId, pacExt, "DR-1");
        String consNewId = "C-NEW-" + UUID.randomUUID();
        String consOldId = "C-OLD-" + UUID.randomUUID();
        fixtures.persistConsulta(consOldId, expHospId,
                LocalDateTime.of(2025, Month.JANUARY, 1, 9, 0), "cefalea", "cefalea tensional");
        fixtures.persistConsulta(consNewId, expHospId,
                LocalDateTime.of(2026, Month.MARCH, 15, 10, 0), "control", "normal");

        given().when().get("/api/patients/" + patientId + "/consultas")
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("[0].id", equalTo(consNewId))
                .body("[1].id", equalTo(consOldId));
    }

    @Test
    @DisplayName("GET /patients/{uuid}/consultas paciente con expediente pero sin consultas → 200 []")
    void getConsultasEmpty() {
        String pacExt = "PAC-EMPTY-" + UUID.randomUUID();
        String patientId = createPatient(pacExt);
        fixtures.persistExpediente("EXP-EMPTY-" + UUID.randomUUID(), pacExt, null);

        given().when().get("/api/patients/" + patientId + "/consultas")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("GET /patients/{uuid}/consultas paciente sin expediente → 200 []")
    void getConsultasNoExpediente() {
        String pacExt = "PAC-NOEXPC-" + UUID.randomUUID();
        String patientId = createPatient(pacExt);

        given().when().get("/api/patients/" + patientId + "/consultas")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("GET /patients/{uuid-inexistente}/consultas → 404 (paciente no existe)")
    void getConsultasPatientNotFound() {
        given().when().get("/api/patients/" + UUID.randomUUID() + "/consultas")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("GET /consultas/{id} existente → 200")
    void getConsultaByIdOk() {
        String expId = "EXP-SINGLE-" + UUID.randomUUID();
        fixtures.persistExpediente(expId, "PAC-SINGLE-" + UUID.randomUUID(), null);
        String consId = "C-SINGLE-" + UUID.randomUUID();
        fixtures.persistConsulta(consId, expId,
                LocalDateTime.of(2026, Month.APRIL, 1, 12, 0), "chequeo", "sano");

        given().when().get("/api/consultas/" + consId)
                .then().statusCode(200)
                .body("id", equalTo(consId))
                .body("expedienteId", equalTo(expId))
                .body("motivoConsulta", equalTo("chequeo"))
                .body("diagnostico", equalTo("sano"));
    }

    @Test
    @DisplayName("GET /consultas/{id-inexistente} → 404")
    void getConsultaByIdNotFound() {
        given().when().get("/api/consultas/MISSING-" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    @DisplayName("POST /patients/{id}/consultas paciente con expediente → 201 y consulta recuperable")
    void createConsultaWithExistingExpediente() {
        String pacExt = "PAC-CREATE-" + UUID.randomUUID();
        String patientId = createPatient(pacExt);
        String expHospId = "EXP-CREATE-" + UUID.randomUUID();
        fixtures.persistExpediente(expHospId, pacExt, "DR-CREATE");

        Map<String, Object> payload = new HashMap<>();
        payload.put("fecha", "2026-04-20T10:30:00");
        payload.put("motivoConsulta", "control");
        payload.put("subjetivo", "paciente refiere mejoría");
        payload.put("objetivo", "TA 120/80");
        payload.put("evaluacion", "estable");
        payload.put("plan", "continuar tratamiento");
        payload.put("prescripcion", "paracetamol 500mg");
        payload.put("diagnostico", "cefalea tensional");

        String createdId = given().contentType(ContentType.JSON).body(payload)
                .when().post("/api/patients/" + patientId + "/consultas")
                .then().statusCode(201)
                .header("Location", containsString("/api/consultas/"))
                .body("id", not(emptyOrNullString()))
                .body("expedienteId", equalTo(expHospId))
                .body("motivoConsulta", equalTo("control"))
                .body("diagnostico", equalTo("cefalea tensional"))
                .body("createdAt", not(emptyOrNullString()))
                .extract().path("id");

        given().when().get("/api/consultas/" + createdId)
                .then().statusCode(200)
                .body("id", equalTo(createdId))
                .body("prescripcion", equalTo("paracetamol 500mg"));
    }

    @Test
    @DisplayName("POST /patients/{id}/consultas paciente sin expediente → 201 con auto-creación de expediente")
    void createConsultaAutoCreatesExpediente() {
        String pacExt = "PAC-AUTO-" + UUID.randomUUID();
        String patientId = createPatient(pacExt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("fecha", "2026-04-20T09:00:00");
        payload.put("motivoConsulta", "primera visita");

        String createdConsultaId = given().contentType(ContentType.JSON).body(payload)
                .when().post("/api/patients/" + patientId + "/consultas")
                .then().statusCode(201)
                .body("id", not(emptyOrNullString()))
                .body("expedienteId", not(emptyOrNullString()))
                .body("motivoConsulta", equalTo("primera visita"))
                .extract().path("id");

        // Verificar que el expediente auto-creado es recuperable por GET /patients/{id}/expediente
        given().when().get("/api/patients/" + patientId + "/expediente")
                .then().statusCode(200)
                .body("pacienteExternoId", equalTo(pacExt));

        // Y que la consulta quedó vinculada
        given().when().get("/api/consultas/" + createdConsultaId)
                .then().statusCode(200);
    }

    @Test
    @DisplayName("POST /patients/{uuid-inexistente}/consultas → 404")
    void createConsultaPatientNotFound() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("fecha", "2026-04-20T10:00:00");
        payload.put("motivoConsulta", "test");

        given().contentType(ContentType.JSON).body(payload)
                .when().post("/api/patients/" + UUID.randomUUID() + "/consultas")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("POST sin fecha (requerida) → 400")
    void createConsultaMissingFecha() {
        String pacExt = "PAC-NOFECHA-" + UUID.randomUUID();
        String patientId = createPatient(pacExt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("motivoConsulta", "test");

        given().contentType(ContentType.JSON).body(payload)
                .when().post("/api/patients/" + patientId + "/consultas")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("POST con solo fecha (todos los campos SOAP null) → 201")
    void createConsultaMinimal() {
        String pacExt = "PAC-MIN-" + UUID.randomUUID();
        String patientId = createPatient(pacExt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("fecha", "2026-04-20T08:00:00");

        given().contentType(ContentType.JSON).body(payload)
                .when().post("/api/patients/" + patientId + "/consultas")
                .then().statusCode(201)
                .body("motivoConsulta", nullValue())
                .body("prescripcion", nullValue());
    }
}
