package itesm.medsync.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import itesm.medsync.domain.chat.exception.ChatException;
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
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class GroqChatGatewayTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpServer server;
    private GroqChatGateway gateway;
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
                512,
                Duration.ofSeconds(2),
                "http://127.0.0.1:" + port);
        gateway = new GroqChatGateway(cfg);
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    private record Response(int status, String body, long delayMillis) {
        static Response ok(String content) {
            String body = "{\"model\":\"llama-3.3-70b-versatile\","
                    + "\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
                    + toJsonString(content) + "}}],"
                    + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5}}";
            return new Response(200, body, 0);
        }
        private static String toJsonString(String s) {
            try { return MAPPER.writeValueAsString(s); } catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    private record RecordedRequest(String authHeader, String body) {}

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
                try { Thread.sleep(next.delayMillis()); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
            byte[] respBytes = next.body() == null ? new byte[0] : next.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(next.status(), respBytes.length == 0 ? -1 : respBytes.length);
            if (respBytes.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) { os.write(respBytes); }
            } else {
                exchange.close();
            }
        }
    }

    @Test
    @DisplayName("Happy path: 200 response returns content string")
    void happyPath() {
        queuedResponses.add(Response.ok("El ibuprofeno es un antiinflamatorio."));

        String result = gateway.complete("system prompt", "¿Qué es el ibuprofeno?");

        assertEquals("El ibuprofeno es un antiinflamatorio.", result);
    }

    @Test
    @DisplayName("Bearer token is sent in Authorization header")
    void authHeaderSent() {
        queuedResponses.add(Response.ok("ok"));

        gateway.complete("sys", "msg");

        assertEquals("Bearer gsk_test_key", recordedRequests.get(0).authHeader());
    }

    @Test
    @DisplayName("response_format is absent in request body (text mode, not JSON mode)")
    void noResponseFormatField() throws Exception {
        queuedResponses.add(Response.ok("ok"));

        gateway.complete("sys", "msg");

        JsonNode body = MAPPER.readTree(recordedRequests.get(0).body());
        assertFalse(body.has("response_format"), "response_format must be absent for text mode");
    }

    @Test
    @DisplayName("System prompt and user message are sent as separate messages")
    void messagesStructure() throws Exception {
        queuedResponses.add(Response.ok("ok"));

        gateway.complete("You are a doctor.", "What is aspirin?");

        JsonNode messages = MAPPER.readTree(recordedRequests.get(0).body()).get("messages");
        assertEquals("system", messages.get(0).get("role").asText());
        assertEquals("You are a doctor.", messages.get(0).get("content").asText());
        assertEquals("user", messages.get(1).get("role").asText());
        assertEquals("What is aspirin?", messages.get(1).get("content").asText());
    }

    @Test
    @DisplayName("HTTP 401 → ChatException mentioning API key")
    void unauthorized401() {
        queuedResponses.add(new Response(401, "{\"error\":\"unauthorized\"}", 0));

        ChatException ex = assertThrows(ChatException.class,
                () -> gateway.complete("sys", "msg"));
        assertTrue(ex.getMessage().contains("401"));
        assertTrue(ex.getMessage().toLowerCase().contains("groq_api_key"));
    }

    @Test
    @DisplayName("HTTP 500 upstream → ChatException")
    void upstream500() {
        queuedResponses.add(new Response(500, "{\"error\":\"server error\"}", 0));

        ChatException ex = assertThrows(ChatException.class,
                () -> gateway.complete("sys", "msg"));
        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    @DisplayName("Timeout → ChatException")
    void timeout() {
        queuedResponses.add(new Response(200, Response.ok("late").body(), 3_000));

        assertThrows(ChatException.class, () -> gateway.complete("sys", "msg"));
    }
}
