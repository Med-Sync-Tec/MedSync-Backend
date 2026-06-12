package itesm.medsync.interfaces.rest.consultaaianalysis;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisException;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisTimeoutException;
import itesm.medsync.domain.hospital.model.Consulta;
import itesm.medsync.domain.hospital.repository.HospitalGateway;
import itesm.medsync.domain.shared.model.ConsultaAnalysisRequest;
import itesm.medsync.domain.shared.model.ConsultaAnalysisResult;
import itesm.medsync.domain.shared.model.ExtractedTag;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.shared.repository.AiAnalysisGateway;
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

import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class AnalyzeConsultaResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    SpecialtyRepository specialtyRepository;

    @InjectMock
    AuthenticatedUserContext userContext;

    /** Mocked so we can drive consulta lookup without seeding the hospital DB. */
    @InjectMock
    HospitalGateway hospitalGateway;

    /** Mocked so the IT does not hit real Groq. */
    @InjectMock
    AiAnalysisGateway aiGateway;

    private UUID cardiologiaId;
    private User medicoConSpecialty;

    @BeforeEach
    void setUp() {
        cardiologiaId = specialtyRepository.findBySlug("cardiologia")
                .map(Specialty::getId).orElseThrow();
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        medicoConSpecialty = userRepository.save(
                User.create("Dra. Ana", "ai-consulta-" + UUID.randomUUID() + "@tec.mx",
                        cardiologiaId, doctorRoleId));
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(medicoConSpecialty, "DOCTOR"));
    }

    private Consulta richConsulta(String id) {
        return new Consulta(
                id, "exp-1", LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0),
                "Dolor torácico", "Refiere disnea", "TA 150/95",
                "HTA estadio 2", "Iniciar Losartán", "Losartán 50mg",
                "I10 Hipertensión", null, null);
    }

    private Consulta blankConsulta(String id) {
        return new Consulta(
                id, "exp-1", LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0),
                null, "", "  ", null, "", null, "   ", null, null);
    }

    private void mockGroqResponse() {
        when(aiGateway.analyzeConsultaText(any(ConsultaAnalysisRequest.class))).thenReturn(
                new ConsultaAnalysisResult(
                        List.of(
                                new ExtractedTag(TipoClinico.ENFERMEDAD, "Hipertensión arterial"),
                                new ExtractedTag(TipoClinico.MEDICAMENTO, "Losartán")),
                        "llama-3.3-70b-versatile", 500, 80));
    }

    private String createPatient() {
        Map<String, Object> p = new HashMap<>();
        p.put("expedienteExternoId", "PAC-AI-" + UUID.randomUUID());
        p.put("nombre", "PT AI");
        p.put("fechaNacimiento", "1990-01-01");
        p.put("genero", "F");
        return given().contentType(ContentType.JSON).body(p)
                .when().post("/api/patients").then().statusCode(201)
                .extract().path("id");
    }

    @Test
    @DisplayName("200 happy path: vocab cardiología poblada → POPULATED con sugerencias")
    void happyPathPopulated() {
        String consultaId = "c-" + UUID.randomUUID();
        when(hospitalGateway.findConsultaById(consultaId)).thenReturn(Optional.of(richConsulta(consultaId)));
        mockGroqResponse();

        String patientId = createPatient();

        given().when().post("/api/consultas/" + consultaId + "/analyze")
                .then().statusCode(200)
                .body("consultaId", equalTo(consultaId))
                .body("especialidadId", equalTo(cardiologiaId.toString()))
                .body("especialidadSlug", equalTo("cardiologia"))
                .body("vocabularyStatus", equalTo("POPULATED"))
                .body("suggestions", hasSize(2))
                .body("suggestions[0].tipo", equalTo("enfermedad"))
                .body("suggestions[0].valor", equalTo("Hipertensión arterial"))
                .body("modelUsed", equalTo("llama-3.3-70b-versatile"))
                .body("promptTokens", equalTo(500))
                .body("completionTokens", equalTo(80));

        // No persistence side-effect on the patient
        given().when().get("/api/patients/" + patientId + "/contextos")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("200 con vocabularyStatus=EMPTY cuando la specialty no tiene términos")
    void emptyVocabularyShortCircuit() {
        // Mover al médico a una specialty con vocabulario vacío. Cualquiera de las
        // 15 specialties con sólo 4 términos sirve poco: necesitamos una con CERO.
        // Como ninguna seed está realmente vacía, creamos una temporal sin JSON.
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        Specialty emptySpec = specialtyRepository.save(
                Specialty.create(
                        "TempEmpty-" + UUID.randomUUID(),
                        "temp-empty-" + UUID.randomUUID().toString().substring(0, 8),
                        "Sin vocabulario"));
        User medicoEmptyVocab = userRepository.save(
                User.create("Dr. Empty", "empty-" + UUID.randomUUID() + "@tec.mx",
                        emptySpec.getId(), doctorRoleId));
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(medicoEmptyVocab, "DOCTOR"));

        String consultaId = "c-empty-" + UUID.randomUUID();
        when(hospitalGateway.findConsultaById(consultaId)).thenReturn(Optional.of(richConsulta(consultaId)));

        given().when().post("/api/consultas/" + consultaId + "/analyze")
                .then().statusCode(200)
                .body("vocabularyStatus", equalTo("EMPTY"))
                .body("suggestions", hasSize(0))
                .body("promptTokens", equalTo(0))
                .body("completionTokens", equalTo(0));
    }

    @Test
    @DisplayName("400 USER_HAS_NO_SPECIALTY cuando el usuario no tiene especialidadId")
    void noSpecialty400() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        User medicoSinSpecialty = userRepository.save(
                User.create("Dr. NoSpec", "nospec-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        when(userContext.getCurrentUser()).thenReturn(new UserWithRole(medicoSinSpecialty, "DOCTOR"));

        given().when().post("/api/consultas/" + UUID.randomUUID() + "/analyze")
                .then().statusCode(400)
                .body("message", containsString("especialidadId"));
    }

    @Test
    @DisplayName("400 cuando la consulta existe pero todos los campos SOAP son blank")
    void invalidConsultaData400() {
        String consultaId = "c-blank-" + UUID.randomUUID();
        when(hospitalGateway.findConsultaById(consultaId)).thenReturn(Optional.of(blankConsulta(consultaId)));

        given().when().post("/api/consultas/" + consultaId + "/analyze")
                .then().statusCode(400)
                .body("message", containsString("no analyzable text"));
    }

    @Test
    @DisplayName("404 cuando el consultaId no existe en el hospital")
    void notFound404() {
        String consultaId = "c-missing-" + UUID.randomUUID();
        when(hospitalGateway.findConsultaById(consultaId)).thenReturn(Optional.empty());

        given().when().post("/api/consultas/" + consultaId + "/analyze")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("401 sin autenticación")
    void unauthorized401() {
        when(userContext.getCurrentUser()).thenReturn(null);

        given().when().post("/api/consultas/anything/analyze")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("502 cuando el gateway lanza AiAnalysisException")
    void badGateway502() {
        String consultaId = "c-boom-" + UUID.randomUUID();
        when(hospitalGateway.findConsultaById(consultaId)).thenReturn(Optional.of(richConsulta(consultaId)));
        when(aiGateway.analyzeConsultaText(any())).thenThrow(new AiAnalysisException("boom"));

        given().when().post("/api/consultas/" + consultaId + "/analyze")
                .then().statusCode(502)
                .body("message", containsString("AI analysis failed"));
    }

    @Test
    @DisplayName("504 cuando el gateway lanza AiAnalysisTimeoutException")
    void timeout504() {
        String consultaId = "c-slow-" + UUID.randomUUID();
        when(hospitalGateway.findConsultaById(consultaId)).thenReturn(Optional.of(richConsulta(consultaId)));
        when(aiGateway.analyzeConsultaText(any())).thenThrow(new AiAnalysisTimeoutException("slow"));

        given().when().post("/api/consultas/" + consultaId + "/analyze")
                .then().statusCode(504)
                .body("message", containsString("timeout"));
    }
}
