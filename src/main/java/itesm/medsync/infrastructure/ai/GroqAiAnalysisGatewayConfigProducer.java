package itesm.medsync.infrastructure.ai;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

/**
 * CDI producer for {@link GroqAiAnalysisGatewayConfig}.
 *
 * Reads the {@code ai.groq.*} keys via MicroProfile Config and hands the
 * validated record to the gateway. The {@code apiKey} default in
 * {@code application.properties} is the empty string, so a missing
 * {@code GROQ_API_KEY} surfaces as the {@code AiConfigurationException}
 * thrown inside the config constructor — failing the boot.
 *
 * Uses {@code @Singleton} (pseudo-scope) instead of {@code @ApplicationScoped}
 * because records are final and CDI cannot create proxies for them.
 */
@ApplicationScoped
public class GroqAiAnalysisGatewayConfigProducer {

    @Produces
    @Singleton
    public GroqAiAnalysisGatewayConfig produce(
            @ConfigProperty(name = "ai.groq.api-key", defaultValue = "") String apiKey,
            @ConfigProperty(name = "ai.groq.model", defaultValue = "llama-3.3-70b-versatile") String model,
            @ConfigProperty(name = "ai.groq.max-tokens", defaultValue = "2048") int maxTokens,
            @ConfigProperty(name = "ai.groq.timeout", defaultValue = "30s") Duration timeout,
            @ConfigProperty(name = "ai.groq.base-url", defaultValue = "https://api.groq.com/openai/v1") String baseUrl) {
        return new GroqAiAnalysisGatewayConfig(apiKey, model, maxTokens, timeout, baseUrl);
    }
}
