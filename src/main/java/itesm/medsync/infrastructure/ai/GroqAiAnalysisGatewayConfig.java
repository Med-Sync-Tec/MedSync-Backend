package itesm.medsync.infrastructure.ai;

import itesm.medsync.domain.articleaianalysis.exception.AiConfigurationException;

import java.time.Duration;
import java.util.Objects;

/**
 * Validated configuration for {@link GroqAiAnalysisGateway}.
 *
 * The constructor enforces the only non-trivial invariant — the API key must
 * be present and non-blank. Defaults for everything else are applied by the
 * config layer (see {@code application.properties}) before this record is
 * built, so once an instance exists, callers can trust it without re-validation.
 *
 * Lives in {@code infrastructure/ai/} because it carries provider-specific
 * fields ({@code baseUrl}, {@code model}) that domain code should not know about.
 */
public final class GroqAiAnalysisGatewayConfig {

    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final Duration timeout;
    private final String baseUrl;

    public GroqAiAnalysisGatewayConfig(String apiKey,
                                       String model,
                                       int maxTokens,
                                       Duration timeout,
                                       String baseUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiConfigurationException(
                    "ai.groq.api-key is missing or blank — set GROQ_API_KEY before starting the application");
        }
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(baseUrl, "baseUrl");
        if (maxTokens <= 0) {
            throw new AiConfigurationException("ai.groq.max-tokens must be > 0, was " + maxTokens);
        }
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.timeout = timeout;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String apiKey() {
        return apiKey;
    }

    public String model() {
        return model;
    }

    public int maxTokens() {
        return maxTokens;
    }

    public Duration timeout() {
        return timeout;
    }

    public String baseUrl() {
        return baseUrl;
    }

    /** Full URL to the Chat Completions endpoint. */
    public String chatCompletionsUrl() {
        return baseUrl + "/chat/completions";
    }
}
