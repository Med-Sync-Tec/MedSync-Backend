package itesm.medsync.interfaces.rest.hospital;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class HospitalResourceIT {

    @Inject
    HospitalTestFixtures fixtures;

    private Map<String, Object> patientPayload(String expedienteExternoId) {
        return Map.of(
                "expedienteExternoId", expedienteExternoId,
                "nombre", "Hospital IT",
                "fechaNacimiento", "1990-05-20",
                "genero", "M",
                "medicoId", UUID.randomUUID().toString()
        );
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
                LocalDateTime.of(2025, 1, 1, 9, 0), "cefalea", "cefalea tensional");
        fixtures.persistConsulta(consNewId, expHospId,
                LocalDateTime.of(2026, 3, 15, 10, 0), "control", "normal");

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
                LocalDateTime.of(2026, 4, 1, 12, 0), "chequeo", "sano");

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
}
