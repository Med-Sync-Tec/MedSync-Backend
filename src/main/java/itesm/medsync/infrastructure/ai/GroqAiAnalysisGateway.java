package itesm.medsync.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisException;
import itesm.medsync.domain.articleaianalysis.exception.AiAnalysisTimeoutException;
import itesm.medsync.domain.shared.model.ArticleAnalysisRequest;
import itesm.medsync.domain.shared.model.ArticleAnalysisResult;
import itesm.medsync.domain.shared.model.ConsultaAnalysisRequest;
import itesm.medsync.domain.shared.model.ConsultaAnalysisResult;
import itesm.medsync.domain.shared.model.ExtractedTag;
import itesm.medsync.domain.shared.model.SpecialtyDescriptor;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.shared.repository.AiAnalysisGateway;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.model.VocabularyTerm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Groq Cloud OpenAI-compatible Chat Completions adapter for the
 * {@link AiAnalysisGateway} port.
 *
 * <h3>Two-step article flow</h3>
 * {@link #analyzeArticle(ArticleAnalysisRequest)} issues two sequential calls:
 * (1) classify into one of the candidate specialties, (2) extract tags from
 * the chosen specialty's vocabulary. Splitting keeps each prompt small enough
 * to comfortably fit Groq's free-tier per-minute token budget.
 *
 * <h3>Consulta flow</h3>
 * {@link #analyzeConsultaText(ConsultaAnalysisRequest)} issues a single
 * extract call against the supplied vocabulary; feature 4 wires it up.
 *
 * <h3>JSON mode</h3>
 * Every request sets {@code response_format: { "type": "json_object" }} so
 * Groq forces a syntactically valid JSON response, removing a class of
 * prose-wrapped-JSON parsing failures.
 */
@ApplicationScoped
public class GroqAiAnalysisGateway implements AiAnalysisGateway {

    private static final Logger LOG = Logger.getLogger(GroqAiAnalysisGateway.class);

    private final GroqAiAnalysisGatewayConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String classifyPromptTemplate;
    private final String extractPromptTemplate;

    @Inject
    public GroqAiAnalysisGateway(GroqAiAnalysisGatewayConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.timeout())
                .build();
        this.objectMapper = new ObjectMapper();
        this.classifyPromptTemplate = loadResource("/prompts/classify-article.txt");
        this.extractPromptTemplate = loadResource("/prompts/extract-tags.txt");
    }

    void onStart(@Observes StartupEvent event) {
        // Trigger construction so the @ApplicationScoped bean's constructor (and
        // through it, the config validation in GroqAiAnalysisGatewayConfig)
        // runs at boot — failing fast when ai.groq.api-key is missing.
        LOG.infof("Groq AI gateway ready: model=%s base-url=%s timeout=%s",
                config.model(), config.baseUrl(), config.timeout());
    }

    @Override
    public ArticleAnalysisResult analyzeArticle(ArticleAnalysisRequest request) {
        if (request.candidateSpecialties() == null || request.candidateSpecialties().isEmpty()) {
            throw new AiAnalysisException("no candidate specialties supplied to gateway");
        }

        // ---- Step 1: classify ----
        String articleText = buildArticleText(request);
        String candidatesJson = buildCandidatesJson(request.candidateSpecialties());
        String classifyPrompt = classifyPromptTemplate
                .replace("{{candidates}}", candidatesJson)
                .replace("{{articleText}}", articleText);

        GroqEnvelope.GroqChatResponse classifyResp = callGroq(classifyPrompt, "classify");
        UUID chosenId = parseSpecialtyId(classifyResp, request.candidateSpecialties());

        // ---- Step 2: extract ----
        Vocabulary vocab = request.vocabulariesById().get(chosenId);
        if (vocab == null) {
            throw new AiAnalysisException("no vocabulary loaded for chosen specialty: " + chosenId);
        }
        String vocabJson = buildVocabularyJson(vocab);
        String extractPrompt = extractPromptTemplate
                .replace("{{especialidadSlug}}", vocab.getEspecialidadSlug())
                .replace("{{vocabulary}}", vocabJson)
                .replace("{{sourceText}}", articleText);

        GroqEnvelope.GroqChatResponse extractResp = callGroq(extractPrompt, "extract-article");
        List<ExtractedTag> tags = parseAndFilterTags(extractResp, vocab);
        if (tags.isEmpty()) {
            throw new AiAnalysisException("no valid tags after vocabulary filter for specialty " + chosenId);
        }

        // Aggregate usage from both calls so the operator sees full cost per analyze invocation.
        int promptTokens = safeUsage(classifyResp).promptTokens() + safeUsage(extractResp).promptTokens();
        int completionTokens = safeUsage(classifyResp).completionTokens() + safeUsage(extractResp).completionTokens();
        String model = extractResp.model() != null ? extractResp.model() : config.model();

        LOG.infof("Groq analyzeArticle ok specialty=%s tags=%d prompt_tokens=%d completion_tokens=%d hadAbstract=%s",
                chosenId, tags.size(), promptTokens, completionTokens, request.hasAbstract());

        return new ArticleAnalysisResult(chosenId, tags, model, promptTokens, completionTokens, request.hasAbstract());
    }

    @Override
    public ConsultaAnalysisResult analyzeConsultaText(ConsultaAnalysisRequest request) {
        if (request.vocabulary() == null) {
            throw new AiAnalysisException("vocabulary required for consulta extraction");
        }
        if (request.consultaText() == null || request.consultaText().isBlank()) {
            throw new AiAnalysisException("consultaText cannot be blank");
        }
        String vocabJson = buildVocabularyJson(request.vocabulary());
        String prompt = extractPromptTemplate
                .replace("{{especialidadSlug}}", request.vocabulary().getEspecialidadSlug())
                .replace("{{vocabulary}}", vocabJson)
                .replace("{{sourceText}}", request.consultaText());

        GroqEnvelope.GroqChatResponse resp = callGroq(prompt, "extract-consulta");
        List<ExtractedTag> tags = parseAndFilterTags(resp, request.vocabulary());
        if (tags.isEmpty()) {
            throw new AiAnalysisException("no valid tags after vocabulary filter for consulta");
        }

        int promptTokens = safeUsage(resp).promptTokens();
        int completionTokens = safeUsage(resp).completionTokens();
        String model = resp.model() != null ? resp.model() : config.model();

        LOG.infof("Groq analyzeConsultaText ok tags=%d prompt_tokens=%d completion_tokens=%d",
                tags.size(), promptTokens, completionTokens);

        return new ConsultaAnalysisResult(tags, model, promptTokens, completionTokens);
    }

    // -------------------------------------------------------------------------
    // Transport
    // -------------------------------------------------------------------------

    private GroqEnvelope.GroqChatResponse callGroq(String userPrompt, String label) {
        GroqEnvelope.GroqChatRequest body = new GroqEnvelope.GroqChatRequest(
                config.model(),
                config.maxTokens(),
                0.0,
                GroqEnvelope.GroqResponseFormat.jsonObject(),
                List.of(
                        new GroqEnvelope.GroqMessage("system",
                                "Respond with a single JSON object matching the schema in the user message. Do not include prose."),
                        new GroqEnvelope.GroqMessage("user", userPrompt)));

        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new AiAnalysisException("failed to serialize Groq request body", ex);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(config.chatCompletionsUrl()))
                .timeout(config.timeout())
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException ex) {
            LOG.errorf(ex, "Groq %s call timed out after %s", label, config.timeout());
            throw new AiAnalysisTimeoutException(
                    "Groq " + label + " call exceeded timeout " + config.timeout(), ex);
        } catch (IOException ex) {
            LOG.errorf(ex, "Groq %s call I/O error", label);
            throw new AiAnalysisException("Groq " + label + " call failed: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiAnalysisException("Groq " + label + " call interrupted", ex);
        }

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            String hint = status == 401 ? " (invalid GROQ_API_KEY?)" : "";
            String bodySnippet = response.body() == null ? "" : truncate(response.body(), 300);
            LOG.errorf("Groq %s call returned HTTP %d: %s", label, status, bodySnippet);
            throw new AiAnalysisException("Groq " + label + " returned HTTP " + status + hint);
        }

        try {
            return objectMapper.readValue(response.body(), GroqEnvelope.GroqChatResponse.class);
        } catch (JsonProcessingException ex) {
            throw new AiAnalysisException("Groq " + label + " returned unparseable envelope", ex);
        }
    }

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    private UUID parseSpecialtyId(GroqEnvelope.GroqChatResponse resp, List<SpecialtyDescriptor> candidates) {
        JsonNode content = readContentJson(resp, "classify");
        JsonNode idNode = content.get("especialidadId");
        if (idNode == null || idNode.isNull() || !idNode.isTextual()) {
            throw new AiAnalysisException("Groq classify response missing 'especialidadId' field");
        }
        UUID parsed;
        try {
            parsed = UUID.fromString(idNode.asText());
        } catch (IllegalArgumentException ex) {
            throw new AiAnalysisException("Groq classify returned non-UUID especialidadId: " + idNode.asText());
        }
        for (SpecialtyDescriptor candidate : candidates) {
            if (candidate.id().equals(parsed)) {
                return parsed;
            }
        }
        throw new AiAnalysisException("Groq classify returned unknown specialty id: " + parsed);
    }

    private List<ExtractedTag> parseAndFilterTags(GroqEnvelope.GroqChatResponse resp, Vocabulary vocab) {
        JsonNode content = readContentJson(resp, "extract");
        JsonNode tagsNode = content.get("tags");
        if (tagsNode == null || !tagsNode.isArray()) {
            throw new AiAnalysisException("Groq extract response missing 'tags' array");
        }
        List<ExtractedTag> kept = new ArrayList<>();
        for (JsonNode tagNode : tagsNode) {
            String tipoRaw = textOrNull(tagNode.get("tipo"));
            String valor = textOrNull(tagNode.get("valor"));
            if (tipoRaw == null || valor == null) {
                LOG.warnf("Groq extract: dropping malformed tag tipo=%s valor=%s", tipoRaw, valor);
                continue;
            }
            TipoClinico tipo;
            try {
                tipo = TipoClinico.valueOf(tipoRaw.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                LOG.warnf("Groq extract: dropping tag with unknown tipo=%s valor=%s", tipoRaw, valor);
                continue;
            }
            if (!vocab.containsTerm(tipo, valor)) {
                LOG.warnf("ai.gateway.unknown_term tipo=%s valor=\"%s\" specialty=%s",
                        tipo, valor, vocab.getEspecialidadSlug());
                continue;
            }
            // Snap to canonical capitalization from the vocabulary.
            String canonical = canonicalValor(vocab, tipo, valor);
            kept.add(new ExtractedTag(tipo, canonical));
        }
        return kept;
    }

    private static String canonicalValor(Vocabulary vocab, TipoClinico tipo, String raw) {
        String normalized = raw.trim().toLowerCase();
        for (VocabularyTerm term : vocab.getTermsFor(tipo)) {
            if (term.getValor().trim().toLowerCase().equals(normalized)) {
                return term.getValor();
            }
        }
        return raw.trim();
    }

    private JsonNode readContentJson(GroqEnvelope.GroqChatResponse resp, String label) {
        if (resp.choices() == null || resp.choices().isEmpty()
                || resp.choices().get(0) == null
                || resp.choices().get(0).message() == null
                || resp.choices().get(0).message().content() == null) {
            throw new AiAnalysisException("Groq " + label + " response missing choices[0].message.content");
        }
        String content = resp.choices().get(0).message().content();
        try {
            return objectMapper.readTree(content);
        } catch (JsonProcessingException ex) {
            throw new AiAnalysisException(
                    "Groq " + label + " response content is not valid JSON: " + truncate(content, 200), ex);
        }
    }

    private static String textOrNull(JsonNode n) {
        if (n == null || n.isNull() || !n.isTextual()) return null;
        String s = n.asText();
        return s.isBlank() ? null : s;
    }

    private GroqEnvelope.GroqUsage safeUsage(GroqEnvelope.GroqChatResponse resp) {
        return resp.usage() != null ? resp.usage() : new GroqEnvelope.GroqUsage(0, 0);
    }

    // -------------------------------------------------------------------------
    // Prompt assembly
    // -------------------------------------------------------------------------

    /**
     * Concatenates whichever of {@code titulo}, {@code abstractText},
     * {@code keywords} are non-blank under labelled headers. Skips blank
     * sections entirely so the prompt does not carry empty headers.
     */
    static String buildArticleText(ArticleAnalysisRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.titulo() != null && !request.titulo().isBlank()) {
            sb.append("[TÍTULO]\n").append(request.titulo().trim()).append("\n\n");
        }
        if (request.abstractText() != null && !request.abstractText().isBlank()) {
            sb.append("[ABSTRACT]\n").append(request.abstractText().trim()).append("\n\n");
        }
        if (request.keywords() != null && !request.keywords().isBlank()) {
            sb.append("[KEYWORDS]\n").append(request.keywords().trim()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String buildCandidatesJson(List<SpecialtyDescriptor> candidates) {
        try {
            List<Map<String, Object>> compact = new ArrayList<>(candidates.size());
            for (SpecialtyDescriptor s : candidates) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", s.id().toString());
                row.put("nombre", s.nombre());
                row.put("slug", s.slug());
                row.put("descripcion", s.descripcion() == null ? "" : s.descripcion());
                compact.add(row);
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(compact);
        } catch (JsonProcessingException ex) {
            throw new AiAnalysisException("failed to serialize specialty candidates", ex);
        }
    }

    private String buildVocabularyJson(Vocabulary vocab) {
        Map<String, List<String>> flat = new LinkedHashMap<>();
        for (TipoClinico tipo : TipoClinico.values()) {
            List<String> valores = new ArrayList<>();
            for (VocabularyTerm term : vocab.getTermsFor(tipo)) {
                valores.add(term.getValor());
            }
            if (!valores.isEmpty()) {
                flat.put(tipo.name(), valores);
            }
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(flat);
        } catch (JsonProcessingException ex) {
            throw new AiAnalysisException("failed to serialize vocabulary", ex);
        }
    }

    // -------------------------------------------------------------------------
    // Resource loading
    // -------------------------------------------------------------------------

    private static String loadResource(String path) {
        try (InputStream in = GroqAiAnalysisGateway.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("prompt resource not found: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read prompt resource: " + path, ex);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
