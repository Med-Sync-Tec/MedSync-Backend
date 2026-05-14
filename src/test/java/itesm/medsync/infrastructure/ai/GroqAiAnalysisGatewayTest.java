package itesm.medsync.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisException;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisTimeoutException;
import itesm.medsync.domain.shared.model.ArticleAnalysisRequest;
import itesm.medsync.domain.shared.model.ArticleAnalysisResult;
import itesm.medsync.domain.shared.model.ConsultaAnalysisRequest;
import itesm.medsync.domain.shared.model.ConsultaAnalysisResult;
import itesm.medsync.domain.shared.model.SpecialtyDescriptor;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.model.VocabularyTerm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class GroqAiAnalysisGatewayTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpServer server;
    private GroqAiAnalysisGateway gateway;
    private final Deque<Response> queuedResponses = new ArrayDeque<>();
    private final List<RecordedRequest> recordedRequests = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        recordedRequests.clear();
        queuedResponses.clear();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", new QueueHandler());
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        int port = server.getAddress().getPort();
        GroqAiAnalysisGatewayConfig cfg = new GroqAiAnalysisGatewayConfig(
                "gsk_test_key",
                "llama-3.3-70b-versatile",
                2048,
                Duration.ofSeconds(2),
                "http://127.0.0.1:" + port);
        gateway = new GroqAiAnalysisGateway(cfg);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private record Response(int status, String body, long delayMillis) {
        static Response ok(String body) {
            return new Response(200, body, 0);
        }
    }

    private record RecordedRequest(String authHeader, String body) {
    }

    private class QueueHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            recordedRequests.add(new RecordedRequest(auth, body));
            Response next = queuedResponses.pollFirst();
            if (next == null) {
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
                return;
            }
            if (next.delayMillis() > 0) {
                try {
                    Thread.sleep(next.delayMillis());
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] respBytes = next.body() == null ? new byte[0] : next.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(next.status(), respBytes.length == 0 ? -1 : respBytes.length);
            if (respBytes.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(respBytes);
                }
            } else {
                exchange.close();
            }
        }
    }

    private static String chatResponse(String contentJson, int promptTokens, int completionTokens) {
        try {
            String contentEscaped = MAPPER.writeValueAsString(contentJson);
            return "{\"model\":\"llama-3.3-70b-versatile\","
                    + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":" + contentEscaped + "}}],"
                    + "\"usage\":{\"prompt_tokens\":" + promptTokens + ",\"completion_tokens\":" + completionTokens + "}}";
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Vocabulary cardioVocab(UUID specialtyId) {
        return new Vocabulary(specialtyId, "cardiologia", "v1",
                Map.of(
                        TipoClinico.ENFERMEDAD,
                        List.of(
                                new VocabularyTerm(TipoClinico.ENFERMEDAD, "Insuficiencia cardíaca congestiva"),
                                new VocabularyTerm(TipoClinico.ENFERMEDAD, "Hipertensión arterial")),
                        TipoClinico.MEDICAMENTO,
                        List.of(new VocabularyTerm(TipoClinico.MEDICAMENTO, "Losartán"))));
    }

    private static ArticleAnalysisRequest article(UUID specialtyId, String titulo, String abs, String keywords) {
        return new ArticleAnalysisRequest(
                titulo, abs, keywords,
                List.of(new SpecialtyDescriptor(specialtyId, "Cardiología", "cardiologia", "El corazón y sus vasos")),
                Map.of(specialtyId, cardioVocab(specialtyId)));
    }

    // -------------------------------------------------------------------------
    // analyzeArticle — happy and degraded paths
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Article full input → 3 tags valid; modelo/tokens y hadAbstract=true")
    void articleHappyPathFull() throws Exception {
        UUID specialtyId = UUID.randomUUID();
        queuedResponses.add(Response.ok(chatResponse(
                "{\"especialidadId\":\"" + specialtyId + "\"}", 100, 20)));
        queuedResponses.add(Response.ok(chatResponse(
                "{\"tags\":[" +
                        "{\"tipo\":\"ENFERMEDAD\",\"valor\":\"Insuficiencia cardíaca congestiva\"}," +
                        "{\"tipo\":\"ENFERMEDAD\",\"valor\":\"Hipertensión arterial\"}," +
                        "{\"tipo\":\"MEDICAMENTO\",\"valor\":\"Losartán\"}]}", 300, 80)));

        ArticleAnalysisResult result = gateway.analyzeArticle(
                article(specialtyId, "Tratamiento de IC", "Resumen sobre IC y losartán.", "ic, losartan"));

        assertEquals(specialtyId, result.especialidadId());
        assertEquals(3, result.tags().size());
        assertTrue(result.hadAbstract());
        assertEquals(400, result.promptTokens());
        assertEquals(100, result.completionTokens());
        assertEquals("llama-3.3-70b-versatile", result.modelUsed());

        // Both requests must include the configured model, max_tokens, JSON mode, and Bearer auth.
        assertEquals(2, recordedRequests.size());
        for (RecordedRequest req : recordedRequests) {
            assertEquals("Bearer gsk_test_key", req.authHeader());
            JsonNode body = MAPPER.readTree(req.body());
            assertEquals("llama-3.3-70b-versatile", body.get("model").asText());
            assertEquals(2048, body.get("max_tokens").asInt());
            assertEquals("json_object", body.get("response_format").get("type").asText());
            String userContent = body.get("messages").get(1).get("content").asText();
            assertTrue(userContent.contains("[TÍTULO]"), "prompt must include TÍTULO");
            assertTrue(userContent.contains("[ABSTRACT]"), "prompt must include ABSTRACT");
            assertTrue(userContent.contains("[KEYWORDS]"), "prompt must include KEYWORDS");
        }
    }

    @Test
    @DisplayName("Article sin abstract → hadAbstract=false; prompt no contiene [ABSTRACT]")
    void articleDegradedNoAbstract() throws Exception {
        UUID specialtyId = UUID.randomUUID();
        queuedResponses.add(Response.ok(chatResponse(
                "{\"especialidadId\":\"" + specialtyId + "\"}", 50, 10)));
        queuedResponses.add(Response.ok(chatResponse(
                "{\"tags\":[{\"tipo\":\"MEDICAMENTO\",\"valor\":\"Losartán\"}]}", 80, 20)));

        ArticleAnalysisResult result = gateway.analyzeArticle(
                article(specialtyId, "Trastuzumab in HER2-low cancer", "  ", "her2, breast"));

        assertFalse(result.hadAbstract());
        JsonNode userMsg = MAPPER.readTree(recordedRequests.get(0).body())
                .get("messages").get(1).get("content");
        String content = userMsg.asText();
        assertTrue(content.contains("[TÍTULO]"));
        assertFalse(content.contains("[ABSTRACT]"), "ABSTRACT header must be absent when input is blank");
        assertTrue(content.contains("[KEYWORDS]"));
    }

    @Test
    @DisplayName("Article título solamente → prompt sólo contiene [TÍTULO]; hadAbstract=false")
    void articleDegradedTitleOnly() throws Exception {
        UUID specialtyId = UUID.randomUUID();
        queuedResponses.add(Response.ok(chatResponse(
                "{\"especialidadId\":\"" + specialtyId + "\"}", 30, 5)));
        queuedResponses.add(Response.ok(chatResponse(
                "{\"tags\":[{\"tipo\":\"ENFERMEDAD\",\"valor\":\"Hipertensión arterial\"}]}", 40, 10)));

        ArticleAnalysisResult result = gateway.analyzeArticle(
                article(specialtyId, "Editorial: New trends", null, null));

        assertFalse(result.hadAbstract());
        String content = MAPPER.readTree(recordedRequests.get(0).body())
                .get("messages").get(1).get("content").asText();
        assertTrue(content.contains("[TÍTULO]"));
        assertFalse(content.contains("[ABSTRACT]"));
        assertFalse(content.contains("[KEYWORDS]"));
    }

    // -------------------------------------------------------------------------
    // analyzeArticle — error mapping
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Classify devuelve UUID que no es candidato → AiAnalysisException")
    void unknownSpecialtyIdRejected() {
        UUID specialtyId = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        queuedResponses.add(Response.ok(chatResponse(
                "{\"especialidadId\":\"" + stranger + "\"}", 1, 1)));

        AiAnalysisException ex = assertThrows(AiAnalysisException.class,
                () -> gateway.analyzeArticle(article(specialtyId, "t", "a", "k")));
        assertTrue(ex.getMessage().contains("unknown specialty id"));
    }

    @Test
    @DisplayName("Classify response sin field especialidadId → AiAnalysisException")
    void classifyMissingFieldRejected() {
        UUID specialtyId = UUID.randomUUID();
        queuedResponses.add(Response.ok(chatResponse("{\"otro\":\"x\"}", 1, 1)));

        AiAnalysisException ex = assertThrows(AiAnalysisException.class,
                () -> gateway.analyzeArticle(article(specialtyId, "t", "a", "k")));
        assertTrue(ex.getMessage().contains("especialidadId"));
    }

    @Test
    @DisplayName("Extract response sin field tags → AiAnalysisException")
    void extractMissingFieldRejected() {
        UUID specialtyId = UUID.randomUUID();
        queuedResponses.add(Response.ok(chatResponse(
                "{\"especialidadId\":\"" + specialtyId + "\"}", 1, 1)));
        queuedResponses.add(Response.ok(chatResponse("{\"otro\":\"x\"}", 1, 1)));

        AiAnalysisException ex = assertThrows(AiAnalysisException.class,
                () -> gateway.analyzeArticle(article(specialtyId, "t", "a", "k")));
        assertTrue(ex.getMessage().contains("tags"));
    }

    @Test
    @DisplayName("3 tags, 2 fuera del vocabulario → 1 tag superviviente")
    void filterUnknownTerms() {
        UUID specialtyId = UUID.randomUUID();
        queuedResponses.add(Response.ok(chatResponse(
                "{\"especialidadId\":\"" + specialtyId + "\"}", 1, 1)));
        queuedResponses.add(Response.ok(chatResponse(
                "{\"tags\":[" +
                        "{\"tipo\":\"ENFERMEDAD\",\"valor\":\"Frobozz syndrome\"}," +
                        "{\"tipo\":\"MEDICAMENTO\",\"valor\":\"FooBarZap\"}," +
                        "{\"tipo\":\"MEDICAMENTO\",\"valor\":\"Losartán\"}]}", 1, 1)));

        ArticleAnalysisResult result = gateway.analyzeArticle(article(specialtyId, "t", "a", "k"));
        assertEquals(1, result.tags().size());
        assertEquals("Losartán", result.tags().get(0).valor());
    }

    @Test
    @DisplayName("Todas las tags filtradas → AiAnalysisException 'no valid tags'")
    void allTagsFiltered() {
        UUID specialtyId = UUID.randomUUID();
        queuedResponses.add(Response.ok(chatResponse(
                "{\"especialidadId\":\"" + specialtyId + "\"}", 1, 1)));
        queuedResponses.add(Response.ok(chatResponse(
                "{\"tags\":[{\"tipo\":\"ENFERMEDAD\",\"valor\":\"Frobozz\"}]}", 1, 1)));

        AiAnalysisException ex = assertThrows(AiAnalysisException.class,
                () -> gateway.analyzeArticle(article(specialtyId, "t", "a", "k")));
        assertTrue(ex.getMessage().contains("no valid tags"));
    }

    @Test
    @DisplayName("HTTP 429 → AiAnalysisException con código en el mensaje")
    void rateLimit429() {
        UUID specialtyId = UUID.randomUUID();
        queuedResponses.add(new Response(429, "{\"error\":\"rate limit\"}", 0));

        AiAnalysisException ex = assertThrows(AiAnalysisException.class,
                () -> gateway.analyzeArticle(article(specialtyId, "t", "a", "k")));
        assertTrue(ex.getMessage().contains("429"));
    }

    @Test
    @DisplayName("HTTP 500 upstream → AiAnalysisException")
    void upstream500() {
        UUID specialtyId = UUID.randomUUID();
        queuedResponses.add(new Response(500, "{\"error\":\"server\"}", 0));

        AiAnalysisException ex = assertThrows(AiAnalysisException.class,
                () -> gateway.analyzeArticle(article(specialtyId, "t", "a", "k")));
        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    @DisplayName("HTTP 401 → AiAnalysisException con pista sobre la API key")
    void unauthorized401() {
        UUID specialtyId = UUID.randomUUID();
        queuedResponses.add(new Response(401, "{\"error\":\"unauthorized\"}", 0));

        AiAnalysisException ex = assertThrows(AiAnalysisException.class,
                () -> gateway.analyzeArticle(article(specialtyId, "t", "a", "k")));
        assertTrue(ex.getMessage().contains("401"));
        assertTrue(ex.getMessage().toLowerCase().contains("groq_api_key"));
    }

    @Test
    @DisplayName("Respuesta excede timeout → AiAnalysisTimeoutException")
    void timeoutMaps() {
        UUID specialtyId = UUID.randomUUID();
        queuedResponses.add(new Response(200, chatResponse("{\"especialidadId\":\"" + specialtyId + "\"}", 1, 1), 3_000));

        assertThrows(AiAnalysisTimeoutException.class,
                () -> gateway.analyzeArticle(article(specialtyId, "t", "a", "k")));
    }

    // -------------------------------------------------------------------------
    // Consulta path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Consulta happy path → 2 tags válidos")
    void consultaHappyPath() {
        UUID specialtyId = UUID.randomUUID();
        Vocabulary vocab = cardioVocab(specialtyId);
        queuedResponses.add(Response.ok(chatResponse(
                "{\"tags\":[" +
                        "{\"tipo\":\"ENFERMEDAD\",\"valor\":\"Hipertensión arterial\"}," +
                        "{\"tipo\":\"MEDICAMENTO\",\"valor\":\"Losartán\"}]}", 150, 30)));

        ConsultaAnalysisResult result = gateway.analyzeConsultaText(
                new ConsultaAnalysisRequest("Paciente con HTA en tratamiento con losartán.", vocab));

        assertEquals(2, result.tags().size());
        assertEquals(150, result.promptTokens());
        assertEquals(30, result.completionTokens());
    }

    @Test
    @DisplayName("Consulta extract sin field tags → AiAnalysisException")
    void consultaMissingFieldRejected() {
        UUID specialtyId = UUID.randomUUID();
        Vocabulary vocab = cardioVocab(specialtyId);
        queuedResponses.add(Response.ok(chatResponse("{\"otro\":\"x\"}", 1, 1)));

        assertThrows(AiAnalysisException.class,
                () -> gateway.analyzeConsultaText(new ConsultaAnalysisRequest("texto", vocab)));
    }

    @Test
    @DisplayName("Consulta con todos los tags fuera del vocabulario → AiAnalysisException")
    void consultaAllFiltered() {
        UUID specialtyId = UUID.randomUUID();
        Vocabulary vocab = cardioVocab(specialtyId);
        queuedResponses.add(Response.ok(chatResponse(
                "{\"tags\":[{\"tipo\":\"ENFERMEDAD\",\"valor\":\"Frobozz\"}]}", 1, 1)));

        assertThrows(AiAnalysisException.class,
                () -> gateway.analyzeConsultaText(new ConsultaAnalysisRequest("texto", vocab)));
    }
}
