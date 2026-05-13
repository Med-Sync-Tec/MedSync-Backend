package itesm.medsync.interfaces.rest.medicamento;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.medicamento.model.MedicamentoEstadoNames;
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
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class MedicamentoResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @InjectMock
    AuthenticatedUserContext userContext;

    @BeforeEach
    void authAsDoctor() {
        UUID doctorRoleId = roleRepository.findByNombre(KnownRoles.DOCTOR)
                .map(Role::getId)
                .orElseThrow(() -> new AssertionError("DOCTOR debe existir (seed V3)"));
        User medico = userRepository.save(
                User.create("Med IT", "med-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(medico, KnownRoles.DOCTOR));
    }

    private Map<String, Object> createPayload(String nombre, String descripcion) {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", nombre);
        m.put("descripcion", descripcion);
        return m;
    }

    @Test
    @DisplayName("POST válido devuelve 201 + Location y body con estado vigente por default")
    void createOk() {
        String nombre = "ZZIT-" + UUID.randomUUID();
        given().contentType(ContentType.JSON)
                .body(createPayload(nombre, "desc"))
                .when().post("/api/medicamentos")
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/medicamentos/"))
                .body("nombre", equalTo(nombre))
                .body("estado", equalTo(MedicamentoEstadoNames.VIGENTE));
    }

    @Test
    @DisplayName("POST con nombre duplicado devuelve 409")
    void createDuplicate() {
        String nombre = "ZZDUP-" + UUID.randomUUID();
        given().contentType(ContentType.JSON).body(createPayload(nombre, "x"))
                .when().post("/api/medicamentos").then().statusCode(201);

        given().contentType(ContentType.JSON).body(createPayload(nombre, "y"))
                .when().post("/api/medicamentos").then().statusCode(409);
    }

    @Test
    @DisplayName("POST con nombre blank devuelve 400")
    void createBlank() {
        given().contentType(ContentType.JSON).body(createPayload("   ", "x"))
                .when().post("/api/medicamentos").then().statusCode(400);
    }

    @Test
    @DisplayName("GET por id existente 200, inexistente 404")
    void getById() {
        String nombre = "ZZGET-" + UUID.randomUUID();
        String id = given().contentType(ContentType.JSON).body(createPayload(nombre, "x"))
                .when().post("/api/medicamentos").then().statusCode(201)
                .extract().path("id");

        given().when().get("/api/medicamentos/" + id).then().statusCode(200)
                .body("nombre", equalTo(nombre));

        given().when().get("/api/medicamentos/" + UUID.randomUUID()).then().statusCode(404);
    }

    @Test
    @DisplayName("PUT actualiza nombre y descripción")
    void update() {
        String nombre = "ZZUPD-" + UUID.randomUUID();
        String id = given().contentType(ContentType.JSON).body(createPayload(nombre, "old"))
                .when().post("/api/medicamentos").then().statusCode(201)
                .extract().path("id");

        Map<String, Object> body = new HashMap<>();
        body.put("nombre", nombre + "-renamed");
        body.put("estado", MedicamentoEstadoNames.VIGENTE);
        body.put("descripcion", "new");

        given().contentType(ContentType.JSON).body(body)
                .when().put("/api/medicamentos/" + id)
                .then().statusCode(200)
                .body("nombre", equalTo(nombre + "-renamed"))
                .body("descripcion", equalTo("new"));
    }

    @Test
    @DisplayName("PATCH cambia estado a obsoleto")
    void patchEstado() {
        String nombre = "ZZPATCH-" + UUID.randomUUID();
        String id = given().contentType(ContentType.JSON).body(createPayload(nombre, "x"))
                .when().post("/api/medicamentos").then().statusCode(201)
                .extract().path("id");

        Map<String, Object> body = new HashMap<>();
        body.put("estado", MedicamentoEstadoNames.OBSOLETO);

        given().contentType(ContentType.JSON).body(body)
                .when().patch("/api/medicamentos/" + id + "/estado")
                .then().statusCode(200)
                .body("estado", equalTo(MedicamentoEstadoNames.OBSOLETO));
    }

    @Test
    @DisplayName("PATCH con estado desconocido devuelve 404")
    void patchUnknownEstado() {
        String nombre = "ZZPATCHX-" + UUID.randomUUID();
        String id = given().contentType(ContentType.JSON).body(createPayload(nombre, "x"))
                .when().post("/api/medicamentos").then().statusCode(201)
                .extract().path("id");

        Map<String, Object> body = new HashMap<>();
        body.put("estado", "inexistente");

        given().contentType(ContentType.JSON).body(body)
                .when().patch("/api/medicamentos/" + id + "/estado")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("DELETE existente 204; segundo DELETE 404")
    void deleteFlow() {
        String nombre = "ZZDEL-" + UUID.randomUUID();
        String id = given().contentType(ContentType.JSON).body(createPayload(nombre, "x"))
                .when().post("/api/medicamentos").then().statusCode(201)
                .extract().path("id");

        given().when().delete("/api/medicamentos/" + id).then().statusCode(204);
        given().when().delete("/api/medicamentos/" + id).then().statusCode(404);
    }

    @Test
    @DisplayName("Sin autenticación → 401 en todos los endpoints")
    void requiresAuth() {
        when(userContext.getCurrentUser()).thenReturn(null);

        given().when().get("/api/medicamentos").then().statusCode(401);
        given().contentType(ContentType.JSON).body(createPayload("x", "y"))
                .when().post("/api/medicamentos").then().statusCode(401);
        given().when().delete("/api/medicamentos/" + UUID.randomUUID()).then().statusCode(401);
    }
}
