package itesm.medsync.infrastructure.ai;

import itesm.medsync.domain.articleaianalysis.exception.AiConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class GroqAiAnalysisGatewayConfigTest {

    @Test
    @DisplayName("api-key null lanza AiConfigurationException")
    void missingApiKey() {
        assertThrows(AiConfigurationException.class,
                () -> new GroqAiAnalysisGatewayConfig(
                        null, "llama-3.3-70b-versatile", 2048,
                        Duration.ofSeconds(30), "https://api.groq.com/openai/v1"));
    }

    @Test
    @DisplayName("api-key blank lanza AiConfigurationException")
    void blankApiKey() {
        assertThrows(AiConfigurationException.class,
                () -> new GroqAiAnalysisGatewayConfig(
                        "   ", "llama-3.3-70b-versatile", 2048,
                        Duration.ofSeconds(30), "https://api.groq.com/openai/v1"));
    }

    @Test
    @DisplayName("config válida no lanza y expone los valores")
    void validConfig() {
        GroqAiAnalysisGatewayConfig cfg = new GroqAiAnalysisGatewayConfig(
                "gsk_test", "llama-3.3-70b-versatile", 2048,
                Duration.ofSeconds(30), "https://api.groq.com/openai/v1");

        assertEquals("gsk_test", cfg.apiKey());
        assertEquals("llama-3.3-70b-versatile", cfg.model());
        assertEquals(2048, cfg.maxTokens());
        assertEquals(Duration.ofSeconds(30), cfg.timeout());
        assertEquals("https://api.groq.com/openai/v1", cfg.baseUrl());
        assertEquals("https://api.groq.com/openai/v1/chat/completions", cfg.chatCompletionsUrl());
    }

    @Test
    @DisplayName("baseUrl con trailing slash se normaliza")
    void trailingSlashStripped() {
        GroqAiAnalysisGatewayConfig cfg = new GroqAiAnalysisGatewayConfig(
                "gsk_test", "llama-3.3-70b-versatile", 2048,
                Duration.ofSeconds(30), "https://api.groq.com/openai/v1/");
        assertEquals("https://api.groq.com/openai/v1", cfg.baseUrl());
    }

    @Test
    @DisplayName("max-tokens <= 0 lanza AiConfigurationException")
    void invalidMaxTokens() {
        assertThrows(AiConfigurationException.class,
                () -> new GroqAiAnalysisGatewayConfig(
                        "gsk", "model", 0, Duration.ofSeconds(30), "https://x"));
    }
}
