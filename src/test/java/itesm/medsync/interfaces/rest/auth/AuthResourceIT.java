package itesm.medsync.interfaces.rest.auth;

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
class AuthResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @InjectMock
    AuthenticatedUserContext userContext;

    private User stubUserWithRole(String roleName) {
        roleRepository.findByNombre(roleName)
                .orElseThrow(() -> new AssertionError(roleName + " role debe existir (seed V3)"));
        return userRepository.save(
                User.create(roleName + " User", roleName.toLowerCase() + "-" + UUID.randomUUID() + "@tec.mx",
                        null, roleRepository.findByNombre(roleName).map(Role::getId).orElseThrow()));
    }

    @BeforeEach
    void resetMock() {
        when(userContext.getCurrentUser()).thenReturn(null);
    }

    @Test
    @DisplayName("expectedRole coincide con el rol real → 200 con UserResponse")
    void loginRoleMatch() {
        User doctor = stubUserWithRole("DOCTOR");
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(doctor, "DOCTOR"));

        Map<String, Object> body = new HashMap<>();
        body.put("expectedRole", "DOCTOR");

        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .body("id", equalTo(doctor.getId().toString()))
                .body("role", equalTo("DOCTOR"));
    }

    @Test
    @DisplayName("expectedRole NO coincide → 403 con actualRole y expectedRole en el body")
    void loginRoleMismatch() {
        User doctor = stubUserWithRole("DOCTOR");
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(doctor, "DOCTOR"));

        Map<String, Object> body = new HashMap<>();
        body.put("expectedRole", "COO");

        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/auth/login")
                .then().statusCode(403)
                .body("actualRole", equalTo("DOCTOR"))
                .body("expectedRole", equalTo("COO"));
    }

    @Test
    @DisplayName("Sin expectedRole → default DOCTOR; usuario DOCTOR pasa, no DOCTOR no")
    void loginDefaultExpectedRole() {
        User doctor = stubUserWithRole("DOCTOR");
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(doctor, "DOCTOR"));

        given().contentType(ContentType.JSON).body(new HashMap<>())
                .when().post("/api/auth/login")
                .then().statusCode(200);
    }

    @Test
    @DisplayName("Sin expectedRole y usuario es COO → 403 (default DOCTOR no coincide)")
    void loginDefaultRejectsCoo() {
        User coo = stubUserWithRole("COO");
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(coo, "COO"));

        given().contentType(ContentType.JSON).body(new HashMap<>())
                .when().post("/api/auth/login")
                .then().statusCode(403)
                .body("actualRole", equalTo("COO"))
                .body("expectedRole", equalTo("DOCTOR"));
    }

    @Test
    @DisplayName("expectedRole case-insensitive (doctor lowercase pasa)")
    void loginCaseInsensitive() {
        User doctor = stubUserWithRole("DOCTOR");
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(doctor, "DOCTOR"));

        Map<String, Object> body = new HashMap<>();
        body.put("expectedRole", "doctor");

        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/auth/login")
                .then().statusCode(200);
    }

    @Test
    @DisplayName("expectedRole con caracteres inválidos → 400 (Pattern violation)")
    void loginInvalidRolePattern() {
        User doctor = stubUserWithRole("DOCTOR");
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(doctor, "DOCTOR"));

        Map<String, Object> body = new HashMap<>();
        body.put("expectedRole", "'; DROP TABLE roles;--");

        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/auth/login")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("Sin usuario en context (sin token) → 401")
    void loginNoAuth() {
        when(userContext.getCurrentUser()).thenReturn(null);

        Map<String, Object> body = new HashMap<>();
        body.put("expectedRole", "DOCTOR");

        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/auth/login")
                .then().statusCode(401);
    }
}
