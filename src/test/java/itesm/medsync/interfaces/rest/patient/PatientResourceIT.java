package itesm.medsync.interfaces.rest.patient;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class PatientResourceIT {

    private Map<String, Object> validPayload(String expediente) {
        return Map.of(
                "expedienteExternoId", expediente,
                "nombre", "Juan Perez",
                "fechaNacimiento", "1990-05-20",
                "genero", "M",
                "medicoId", UUID.randomUUID().toString()
        );
    }

    @Test
    @DisplayName("POST válido devuelve 201 con body y header Location")
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
                .body("activo", equalTo(true))
                .body("createdAt", not(emptyOrNullString()));
    }

    @Test
    @DisplayName("POST con nombre blank devuelve 400")
    void createBlankNombre() {
        Map<String, Object> payload = Map.of(
                "expedienteExternoId", "IT-" + UUID.randomUUID(),
                "nombre", "   ",
                "fechaNacimiento", "1990-01-01",
                "medicoId", UUID.randomUUID().toString()
        );
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/api/patients")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("POST con fechaNacimiento futura devuelve 400")
    void createFutureFecha() {
        Map<String, Object> payload = Map.of(
                "expedienteExternoId", "IT-" + UUID.randomUUID(),
                "nombre", "Juan",
                "fechaNacimiento", LocalDate.now().plusDays(1).toString(),
                "medicoId", UUID.randomUUID().toString()
        );
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/api/patients")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("POST sin genero (opcional) devuelve 201")
    void createWithoutGenero() {
        Map<String, Object> payload = Map.of(
                "expedienteExternoId", "IT-NOGEN-" + UUID.randomUUID(),
                "nombre", "Ana",
                "fechaNacimiento", "1985-03-03",
                "medicoId", UUID.randomUUID().toString()
        );
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
