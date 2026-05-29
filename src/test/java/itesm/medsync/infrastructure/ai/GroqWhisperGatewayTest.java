package itesm.medsync.infrastructure.ai;

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

class GroqWhisperGatewayTest {

    private HttpServer server;
    private GroqWhisperGateway gateway;
    private final Deque<Response> queuedResponses = new ArrayDeque<>();
    private final List<RecordedRequest> recordedRequests = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        recordedRequests.clear();
        queuedResponses.clear();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/audio/transcriptions", new QueueHandler());
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        int port = server.getAddress().getPort();
        GroqAiAnalysisGatewayConfig cfg = new GroqAiAnalysisGatewayConfig(
                "gsk_test_key",
                "whisper-large-v3",
                512,
                Duration.ofSeconds(2),
                "http://127.0.0.1:" + port);
        gateway = new GroqWhisperGateway(cfg);
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    private record Response(int status, String body) {}
    private record RecordedRequest(String authHeader, String contentType, byte[] body) {}

    private class QueueHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = exchange.getRequestBody().readAllBytes();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            String ct = exchange.getRequestHeaders().getFirst("Content-Type");
            recordedRequests.add(new RecordedRequest(auth, ct, body));

            Response next = queuedResponses.pollFirst();
            if (next == null) {
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
                return;
            }
            byte[] resp = next.body() == null ? new byte[0] : next.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(next.status(), resp.length == 0 ? -1 : resp.length);
            if (resp.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
            } else {
                exchange.close();
            }
        }
    }

    @Test
    @DisplayName("Happy path: returns transcript as plain text")
    void happyPath() {
        queuedResponses.add(new Response(200, "Paciente con dolor abdominal de tres días."));

        String result = gateway.transcribe("audio".getBytes(StandardCharsets.UTF_8), "audio/webm");

        assertEquals("Paciente con dolor abdominal de tres días.", result);
    }

    @Test
    @DisplayName("Bearer token is sent in Authorization header")
    void authHeaderSent() {
        queuedResponses.add(new Response(200, "ok"));

        gateway.transcribe("bytes".getBytes(), "audio/webm");

        assertEquals("Bearer gsk_test_key", recordedRequests.get(0).authHeader());
    }

    @Test
    @DisplayName("Content-Type is multipart/form-data with boundary")
    void contentTypeIsMultipart() {
        queuedResponses.add(new Response(200, "ok"));

        gateway.transcribe("bytes".getBytes(), "audio/webm");

        String ct = recordedRequests.get(0).contentType();
        assertTrue(ct.startsWith("multipart/form-data"), "Content-Type must be multipart/form-data, was: " + ct);
        assertTrue(ct.contains("boundary="), "Content-Type must contain boundary");
    }

    @Test
    @DisplayName("Request body contains model and language fields")
    void bodyContainsRequiredFields() {
        queuedResponses.add(new Response(200, "ok"));

        gateway.transcribe("bytes".getBytes(), "audio/webm");

        String body = new String(recordedRequests.get(0).body(), StandardCharsets.UTF_8);
        assertTrue(body.contains("whisper-large-v3"), "body must include model name");
        assertTrue(body.contains("language"), "body must include language field");
        assertTrue(body.contains("es"), "body must include 'es' for Spanish");
    }

    @Test
    @DisplayName("HTTP 401 → ChatException mentioning API key")
    void unauthorized401() {
        queuedResponses.add(new Response(401, "{\"error\":\"unauthorized\"}"));

        ChatException ex = assertThrows(ChatException.class,
                () -> gateway.transcribe("bytes".getBytes(), "audio/webm"));
        assertTrue(ex.getMessage().contains("401"));
        assertTrue(ex.getMessage().toLowerCase().contains("groq_api_key"));
    }

    @Test
    @DisplayName("HTTP 500 → ChatException")
    void upstream500() {
        queuedResponses.add(new Response(500, "{\"error\":\"server\"}"));

        ChatException ex = assertThrows(ChatException.class,
                () -> gateway.transcribe("bytes".getBytes(), "audio/webm"));
        assertTrue(ex.getMessage().contains("500"));
    }
}
