package itesm.medsync.application.soapdictation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import itesm.medsync.domain.chat.exception.ChatException;
import itesm.medsync.domain.chat.repository.ChatGateway;
import itesm.medsync.domain.soapdictation.model.SOAPDictationResult;
import itesm.medsync.domain.soapdictation.repository.WhisperGateway;
import itesm.medsync.domain.soapdictation.usecase.TranscribeAndExtractSOAPUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TranscribeAndExtractSOAPService implements TranscribeAndExtractSOAPUseCase {

    static final String SYSTEM_PROMPT = """
            Eres un asistente médico. Dado el siguiente dictado en español, extrae y
            clasifica la información en los campos del formato SOAP.

            Responde ÚNICAMENTE con un JSON con esta estructura exacta:
            {
              "motivoConsulta": "...",
              "subjetivo": "...",
              "objetivo": "...",
              "evaluacion": "...",
              "diagnostico": "...",
              "plan": "...",
              "prescripcion": "..."
            }

            Reglas:
            - Usa null para los campos que no puedas inferir del dictado.
            - No inventes información que no esté en el dictado.
            - Responde en español.
            """;

    private final WhisperGateway whisperGateway;
    private final ChatGateway chatGateway;
    private final ObjectMapper objectMapper;

    @Inject
    public TranscribeAndExtractSOAPService(WhisperGateway whisperGateway, ChatGateway chatGateway) {
        this.whisperGateway = whisperGateway;
        this.chatGateway = chatGateway;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public SOAPDictationResult transcribeAndExtract(byte[] audioBytes, String mimeType) {
        String transcript = whisperGateway.transcribe(audioBytes, mimeType);
        String jsonResponse = chatGateway.completeJson(SYSTEM_PROMPT, transcript);

        try {
            JsonNode node = objectMapper.readTree(jsonResponse);
            return new SOAPDictationResult(
                    textOrNull(node, "motivoConsulta"),
                    textOrNull(node, "subjetivo"),
                    textOrNull(node, "objetivo"),
                    textOrNull(node, "evaluacion"),
                    textOrNull(node, "diagnostico"),
                    textOrNull(node, "plan"),
                    textOrNull(node, "prescripcion")
            );
        } catch (JsonProcessingException ex) {
            throw new ChatException("Failed to parse SOAP extraction response", ex);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return null;
        String s = n.asText().trim();
        return s.isEmpty() ? null : s;
    }
}
