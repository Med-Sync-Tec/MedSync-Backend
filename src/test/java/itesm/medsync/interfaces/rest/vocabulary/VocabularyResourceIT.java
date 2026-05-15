package itesm.medsync.interfaces.rest.vocabulary;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
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

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class VocabularyResourceIT {

    private static final UUID CARDIOLOGIA_ID = UUID.fromString("00000000-0000-2000-8000-000000000001");

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
                        roleName.toLowerCase() + "-vocab-" + UUID.randomUUID() + "@tec.mx",
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
    @DisplayName("GET /api/especialidades/{id}/vocabulary sin auth → 401")
    void unauthenticated() {
        given().when().get("/api/especialidades/" + CARDIOLOGIA_ID + "/vocabulary")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("GET como DOCTOR → 403")
    void forbidden() {
        authenticatedAs(KnownRoles.DOCTOR);
        given().when().get("/api/especialidades/" + CARDIOLOGIA_ID + "/vocabulary")
                .then().statusCode(403);
    }

    @Test
    @DisplayName("GET con id desconocido → 404")
    void notFound() {
        authenticatedAs(KnownRoles.COO);
        given().when().get("/api/especialidades/" + UUID.randomUUID() + "/vocabulary")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("GET como COO para cardiologia (con vocabulary cargado) → 200 con los 4 buckets")
    void okWithLoadedVocabulary() {
        authenticatedAs(KnownRoles.COO);
        given().when().get("/api/especialidades/" + CARDIOLOGIA_ID + "/vocabulary")
                .then().statusCode(200)
                .body("especialidadSlug", equalTo("cardiologia"))
                .body("version", equalTo("2026-05-12"))
                .body("termsByType", hasKey("ENFERMEDAD"))
                .body("termsByType", hasKey("SINTOMA"))
                .body("termsByType", hasKey("TRATAMIENTO"))
                .body("termsByType", hasKey("MEDICAMENTO"))
                .body("totalTerms", greaterThan(0));
    }

    @Test
    @DisplayName("GET como COO para una especialidad stub → 200 con vocabulary stub")
    void okWithStubVocabulary() {
        authenticatedAs(KnownRoles.COO);
        // Endocrinología — stubbed
        UUID id = UUID.fromString("00000000-0000-2000-8000-000000000002");
        given().when().get("/api/especialidades/" + id + "/vocabulary")
                .then().statusCode(200)
                .body("especialidadSlug", equalTo("endocrinologia"))
                .body("version", equalTo("2026-05-12-stub"))
                .body("totalTerms", equalTo(4));
    }
}
