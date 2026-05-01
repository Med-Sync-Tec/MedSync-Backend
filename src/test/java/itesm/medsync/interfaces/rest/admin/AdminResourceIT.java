package itesm.medsync.interfaces.rest.admin;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.user.model.KnownRoles;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.domain.user.repository.FirebaseUserGateway;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class AdminResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @InjectMock
    AuthenticatedUserContext userContext;

    @InjectMock
    FirebaseUserGateway firebaseUserGateway;

    private User stubUser(String roleName) {
        UUID roleId = roleRepository.findByNombre(roleName).map(Role::getId)
                .orElseThrow(() -> new AssertionError(roleName + " debe existir (seed V3)"));
        return userRepository.save(
                User.create(roleName + " caller",
                        roleName.toLowerCase() + "-admin-" + UUID.randomUUID() + "@tec.mx",
                        roleId));
    }

    @BeforeEach
    void resetMocks() {
        when(userContext.getCurrentUser()).thenReturn(null);
        when(firebaseUserGateway.createUser(any(), any(), any()))
                .thenReturn("fb-uid-" + UUID.randomUUID());
    }

    private Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("correo", "nuevo-" + UUID.randomUUID() + "@tec.mx");
        body.put("nombre", "Nuevo");
        body.put("password", "secret123");
        body.put("rol", KnownRoles.DOCTOR);
        return body;
    }

    @Test
    @DisplayName("Sin auth (userContext null) → 401")
    void unauthenticated() {
        given().contentType(ContentType.JSON).body(validBody())
                .when().post("/api/admin/users")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("Caller con rol distinto a COO → 403")
    void notCoo() {
        User doctor = stubUser(KnownRoles.DOCTOR);
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(doctor, KnownRoles.DOCTOR));

        given().contentType(ContentType.JSON).body(validBody())
                .when().post("/api/admin/users")
                .then().statusCode(403);
    }

    @Test
    @DisplayName("Caller COO con datos válidos → 201 con Location")
    void createOk() {
        User coo = stubUser(KnownRoles.COO);
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(coo, KnownRoles.COO));

        given().contentType(ContentType.JSON).body(validBody())
                .when().post("/api/admin/users")
                .then().statusCode(201)
                .header("Location", containsString("/api/admin/users/"));
    }

    @Test
    @DisplayName("Caller COO con email duplicado → 409")
    void createDuplicateEmail() {
        User coo = stubUser(KnownRoles.COO);
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(coo, KnownRoles.COO));

        Map<String, Object> body = validBody();
        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/admin/users").then().statusCode(201);

        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/admin/users").then().statusCode(409);
    }

    @Test
    @DisplayName("Caller COO con rol desconocido → 400")
    void unknownRole() {
        User coo = stubUser(KnownRoles.COO);
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(coo, KnownRoles.COO));

        Map<String, Object> body = validBody();
        body.put("rol", "CHEF");

        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/admin/users").then().statusCode(400);
    }
}
