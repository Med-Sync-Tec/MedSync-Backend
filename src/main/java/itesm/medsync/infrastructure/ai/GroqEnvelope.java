package itesm.medsync.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Jackson-mapped Groq Cloud / OpenAI Chat Completions envelope types.
 *
 * Package-private — these never escape {@code infrastructure/ai/}. The domain
 * port {@code AiAnalysisGateway} speaks in {@code ArticleAnalysisResult} /
 * {@code ConsultaAnalysisResult}, so a future provider swap stays local.
 */
final class GroqEnvelope {

    private GroqEnvelope() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GroqChatRequest(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            double temperature,
            @JsonProperty("response_format") GroqResponseFormat responseFormat,
            List<GroqMessage> messages) {
    }

    record GroqMessage(String role, String content) {
    }

    record GroqResponseFormat(String type) {
        static GroqResponseFormat jsonObject() {
            return new GroqResponseFormat("json_object");
        }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    record GroqChatResponse(
            String model,
            List<GroqChoice> choices,
            GroqUsage usage) {
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    record GroqChoice(GroqMessage message) {
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    record GroqUsage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens) {
    }
}
