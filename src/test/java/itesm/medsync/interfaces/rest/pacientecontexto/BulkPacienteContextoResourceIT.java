package itesm.medsync.interfaces.rest.pacientecontexto;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class BulkPacienteContextoResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    PatientRepository patientRepository;

    @Inject
    SpecialtyRepository specialtyRepository;

    @InjectMock
    AuthenticatedUserContext userContext;

    private UUID patientId;
    private UUID cardiologiaId;

    @BeforeEach
    void setUp() {
        cardiologiaId = specialtyRepository.findBySlug("cardiologia")
                .map(Specialty::getId).orElseThrow();
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        User medico = userRepository.save(
                User.create("Bulk IT", "bulk-" + UUID.randomUUID() + "@tec.mx",
                        cardiologiaId, doctorRoleId));
        Patient p = patientRepository.save(
                Patient.create("EXP-BULK-" + UUID.randomUUID(),
                        "Bulk Pte", LocalDate.of(1990, Month.JANUARY, 1), "F", medico.getId()));
        patientId = p.getId();
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(medico, "DOCTOR"));
    }

    private static Map<String, Object> entry(String tipo, String valor) {
        Map<String, Object> m = new HashMap<>();
        m.put("tipo", tipo);
        m.put("valor", valor);
        return m;
    }

    private static Map<String, Object> bulkBody(List<Map<String, Object>> entries) {
        Map<String, Object> body = new HashMap<>();
        body.put("entries", entries);
        return body;
    }

    @Test
    @DisplayName("201 happy path: 3 entries → todos persistidos con la especialidad del caller")
    void bulkHappyPath() {
        List<Map<String, Object>> entries = List.of(
                entry("enfermedad", "Hipertensión arterial"),
                entry("medicamento", "Losartán"),
                entry("sintoma", "Disnea de esfuerzo"));

        given().contentType(ContentType.JSON).body(bulkBody(entries))
                .when().post("/api/patients/" + patientId + "/contextos/bulk")
                .then().statusCode(201)
                .body("$", hasSize(3))
                .body("[0].pacienteId", equalTo(patientId.toString()))
                .body("[0].especialidadId", equalTo(cardiologiaId.toString()))
                .body("[1].tipo", equalTo("medicamento"))
                .body("[2].valor", equalTo("Disnea de esfuerzo"));

        // Verify persistence
        given().when().get("/api/patients/" + patientId + "/contextos")
                .then().statusCode(200)
                .body("$", hasSize(3));
    }

    @Test
    @DisplayName("400 cuando entries está vacío")
    void emptyEntries400() {
        given().contentType(ContentType.JSON).body(bulkBody(List.of()))
                .when().post("/api/patients/" + patientId + "/contextos/bulk")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("400 cuando entries tiene 51 items (cap @Size(max=50))")
    void over50Entries400() {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            entries.add(entry("enfermedad", "Tag " + i));
        }

        given().contentType(ContentType.JSON).body(bulkBody(entries))
                .when().post("/api/patients/" + patientId + "/contextos/bulk")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("400 cuando un entry tiene tipo desconocido; 0 filas persistidas (rollback)")
    void invalidTipoRollsBack() {
        List<Map<String, Object>> entries = List.of(
                entry("enfermedad", "Hipertensión arterial"),
                entry("not-a-tipo", "boom"),
                entry("medicamento", "Losartán"));

        given().contentType(ContentType.JSON).body(bulkBody(entries))
                .when().post("/api/patients/" + patientId + "/contextos/bulk")
                .then().statusCode(400);

        given().when().get("/api/patients/" + patientId + "/contextos")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("400 cuando un valor excede 500 caracteres")
    void valorTooLong400() {
        List<Map<String, Object>> entries = List.of(
                entry("enfermedad", "x".repeat(501)));

        given().contentType(ContentType.JSON).body(bulkBody(entries))
                .when().post("/api/patients/" + patientId + "/contextos/bulk")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("404 cuando el patientId no existe")
    void patientNotFound404() {
        List<Map<String, Object>> entries = List.of(entry("enfermedad", "x"));

        given().contentType(ContentType.JSON).body(bulkBody(entries))
                .when().post("/api/patients/" + UUID.randomUUID() + "/contextos/bulk")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("401 sin autenticación")
    void unauthorized401() {
        when(userContext.getCurrentUser()).thenReturn(null);
        List<Map<String, Object>> entries = List.of(entry("enfermedad", "x"));

        given().contentType(ContentType.JSON).body(bulkBody(entries))
                .when().post("/api/patients/" + patientId + "/contextos/bulk")
                .then().statusCode(401);
    }
}
