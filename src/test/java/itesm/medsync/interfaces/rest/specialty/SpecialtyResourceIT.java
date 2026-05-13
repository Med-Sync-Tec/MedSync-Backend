package itesm.medsync.interfaces.rest.specialty;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import itesm.medsync.application.security.AuthenticatedUserContext;
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
class SpecialtyResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @InjectMock
    AuthenticatedUserContext userContext;

    private User stubUser(String roleName) {
        UUID roleId = roleRepository.findByNombre(roleName).map(Role::getId)
                .orElseThrow(() -> new AssertionError(roleName + " debe existir (seed V3)"));
        return userRepository.save(
                User.create(roleName + " caller",
                        roleName.toLowerCase() + "-spec-" + UUID.randomUUID() + "@tec.mx",
                        null, roleId));
    }

    @BeforeEach
    void resetMocks() {
        when(userContext.getCurrentUser()).thenReturn(null);
    }

    private void authenticatedAs(String roleName) {
        User u = stubUser(roleName);
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(u, roleName));
    }

    @Test
    @DisplayName("GET /api/especialidades sin auth → 401")
    void listUnauthenticated() {
        given().when().get("/api/especialidades").then().statusCode(401);
    }

    @Test
    @DisplayName("GET /api/especialidades autenticado → 200 con las seeded specialties")
    void listOk() {
        authenticatedAs(KnownRoles.DOCTOR);
        given().when().get("/api/especialidades")
                .then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(16))
                .body("nombre", hasItems("Cardiología", "Endocrinología", "Medicina Interna"));
    }

    @Test
    @DisplayName("GET /api/especialidades/{id} con id seeded → 200")
    void getByIdSeeded() {
        authenticatedAs(KnownRoles.DOCTOR);
        // Cardiología seed
        UUID id = UUID.fromString("00000000-0000-2000-8000-000000000001");
        given().when().get("/api/especialidades/" + id)
                .then().statusCode(200)
                .body("nombre", equalTo("Cardiología"))
                .body("slug", equalTo("cardiologia"));
    }

    @Test
    @DisplayName("GET /api/especialidades/{id} con id desconocido → 404")
    void getByIdUnknown() {
        authenticatedAs(KnownRoles.DOCTOR);
        given().when().get("/api/especialidades/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    @DisplayName("POST /api/admin/especialidades sin auth → 401")
    void createUnauthenticated() {
        given().contentType(ContentType.JSON).body(validBody("Test", "test-slug-1"))
                .when().post("/api/admin/especialidades")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("POST /api/admin/especialidades como DOCTOR → 403")
    void createNotCoo() {
        authenticatedAs(KnownRoles.DOCTOR);
        given().contentType(ContentType.JSON).body(validBody("Test", "test-slug-2"))
                .when().post("/api/admin/especialidades")
                .then().statusCode(403);
    }

    @Test
    @DisplayName("POST /api/admin/especialidades como COO → 201 con Location")
    void createOk() {
        authenticatedAs(KnownRoles.COO);
        String uniqueSlug = "test-slug-" + System.nanoTime();
        given().contentType(ContentType.JSON).body(validBody("Nueva " + uniqueSlug, uniqueSlug))
                .when().post("/api/admin/especialidades")
                .then().statusCode(201)
                .header("Location", containsString("/api/admin/especialidades/"))
                .body("slug", equalTo(uniqueSlug));
    }

    @Test
    @DisplayName("POST con slug inválido (regex) → 400")
    void createBadSlug() {
        authenticatedAs(KnownRoles.COO);
        given().contentType(ContentType.JSON).body(validBody("X", "Bad Slug!"))
                .when().post("/api/admin/especialidades")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("POST con slug seeded → 409")
    void createDuplicateSlug() {
        authenticatedAs(KnownRoles.COO);
        given().contentType(ContentType.JSON).body(validBody("X", "cardiologia"))
                .when().post("/api/admin/especialidades")
                .then().statusCode(409);
    }

    @Test
    @DisplayName("POST con nombre seeded activo → 409")
    void createDuplicateNombre() {
        authenticatedAs(KnownRoles.COO);
        given().contentType(ContentType.JSON).body(validBody("Cardiología", "cardiologia-x-1"))
                .when().post("/api/admin/especialidades")
                .then().statusCode(409);
    }

    @Test
    @DisplayName("DELETE como COO → 204")
    void deleteOk() {
        authenticatedAs(KnownRoles.COO);
        // Create one then soft-delete it
        String slug = "to-delete-" + System.nanoTime();
        UUID created = UUID.fromString(
                given().contentType(ContentType.JSON).body(validBody("ToDelete " + slug, slug))
                        .when().post("/api/admin/especialidades")
                        .then().statusCode(201)
                        .extract().path("id"));

        given().when().delete("/api/admin/especialidades/" + created)
                .then().statusCode(204);

        // Confirm GET returns 404 (filtered by @SQLRestriction)
        given().when().get("/api/especialidades/" + created)
                .then().statusCode(404);
    }

    @Test
    @DisplayName("DELETE id desconocido → 404")
    void deleteUnknown() {
        authenticatedAs(KnownRoles.COO);
        given().when().delete("/api/admin/especialidades/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    private Map<String, Object> validBody(String nombre, String slug) {
        Map<String, Object> body = new HashMap<>();
        body.put("nombre", nombre);
        body.put("slug", slug);
        body.put("descripcion", "Test");
        return body;
    }
}
