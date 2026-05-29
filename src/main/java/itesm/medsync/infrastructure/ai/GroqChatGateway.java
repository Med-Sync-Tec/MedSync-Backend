package itesm.medsync.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import itesm.medsync.domain.articleaianalysis.exception.AiConfigurationException;
import itesm.medsync.domain.chat.exception.ChatException;
import itesm.medsync.domain.chat.repository.ChatGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Groq Cloud adapter for the {@link ChatGateway} port.
 *
 * Uses text mode (no response_format) so the model can reply in natural language
 * instead of being forced into a JSON object. Reuses {@link GroqAiAnalysisGatewayConfig}
 * and {@link GroqEnvelope} from the existing AI infrastructure.
 */
@ApplicationScoped
public class GroqChatGateway implements ChatGateway {

    private static final Logger LOG = Logger.getLogger(GroqChatGateway.class);

    private final GroqAiAnalysisGatewayConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public GroqChatGateway(GroqAiAnalysisGatewayConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.timeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String complete(String systemPrompt, String userMessage) {
        return callGroqChat(systemPrompt, userMessage, null);
    }

    @Override
    public String completeJson(String systemPrompt, String userMessage) {
        return callGroqChat(systemPrompt, userMessage, GroqEnvelope.GroqResponseFormat.jsonObject());
    }

    private String callGroqChat(String systemPrompt, String userMessage,
                                 GroqEnvelope.GroqResponseFormat responseFormat) {
        if (!config.isConfigured()) {
            throw new AiConfigurationException(
                    "GROQ_API_KEY is not set — set the environment variable to use MediBot");
        }
        GroqEnvelope.GroqChatRequest body = new GroqEnvelope.GroqChatRequest(
                config.model(),
                config.maxTokens(),
                responseFormat == null ? 0.7 : 0.0,
                responseFormat,
                List.of(
                        new GroqEnvelope.GroqMessage("system", systemPrompt),
                        new GroqEnvelope.GroqMessage("user", userMessage)));

        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new ChatException("Failed to serialize chat request", ex);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.chatCompletionsUrl()))
                .timeout(config.timeout())
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException ex) {
            LOG.errorf(ex, "Groq chat call timed out after %s", config.timeout());
            throw new ChatException("Groq chat call exceeded timeout " + config.timeout(), ex);
        } catch (IOException ex) {
            LOG.errorf(ex, "Groq chat call I/O error: %s", ex.getClass().getName());
            throw new ChatException("Groq chat call failed [" + ex.getClass().getSimpleName() + "]: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ChatException("Groq chat call interrupted", ex);
        }

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            String hint = status == 401 ? " (invalid GROQ_API_KEY?)" : "";
            LOG.errorf("Groq chat returned HTTP %d", status);
            throw new ChatException("Groq chat returned HTTP " + status + hint);
        }

        try {
            GroqEnvelope.GroqChatResponse envelope =
                    objectMapper.readValue(response.body(), GroqEnvelope.GroqChatResponse.class);
            if (envelope.choices() == null || envelope.choices().isEmpty()
                    || envelope.choices().get(0).message() == null
                    || envelope.choices().get(0).message().content() == null) {
                throw new ChatException("Groq chat response missing choices[0].message.content");
            }
            return envelope.choices().get(0).message().content();
        } catch (JsonProcessingException ex) {
            throw new ChatException("Groq chat returned unparseable envelope", ex);
        }
    }
}
