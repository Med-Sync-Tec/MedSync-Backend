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
public record GroqAiAnalysisGatewayConfig(
        String apiKey,
        String model,
        int maxTokens,
        Duration timeout,
        String baseUrl) {

    // Compact constructor: validate, then normalize baseUrl trailing slash
    public GroqAiAnalysisGatewayConfig {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(baseUrl, "baseUrl");
        if (maxTokens <= 0) {
            throw new AiConfigurationException("ai.groq.max-tokens must be > 0, was " + maxTokens);
        }
        // Remove trailing slash so chatCompletionsUrl() always produces a clean URL
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** Returns {@code true} when the API key is present and non-blank. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("unconfigured");
    }

    /** Full URL to the Chat Completions endpoint. */
    public String chatCompletionsUrl() {
        return baseUrl + "/chat/completions";
    }
}
