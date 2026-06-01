package itesm.medsync.application.chat;

import itesm.medsync.domain.chat.model.ChatRequest;
import itesm.medsync.domain.chat.model.ChatResponse;
import itesm.medsync.domain.chat.repository.ChatGateway;
import itesm.medsync.domain.chat.usecase.ChatUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChatService implements ChatUseCase {

    static final String CRITICAL_PREFIX = "CRITICAL:";

    static final String CANNED_CRITICAL =
            "Para situaciones de emergencia o decisiones clínicas críticas, " +
            "consulta los protocolos institucionales y al personal de guardia.";

    static final String SYSTEM_PROMPT = """
            Eres MediBot, un asistente de apoyo para profesionales de la salud.
            Puedes responder preguntas médicas generales, sobre medicamentos comunes,
            procedimientos estándar o literatura médica.

            RESTRICCIONES:
            - Si la pregunta implica una emergencia activa, una decisión clínica crítica
              o un diagnóstico definitivo, responde EXACTAMENTE con la línea:
              CRITICAL: Para situaciones de emergencia o decisiones clínicas críticas, \
            consulta los protocolos institucionales y al personal de guardia.
            - No inventes datos de pacientes reales.
            - Responde siempre en español.
            - Sé conciso (máximo 3 párrafos).
            """;

    private final ChatGateway gateway;

    @Inject
    public ChatService(ChatGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String raw = gateway.complete(SYSTEM_PROMPT, request.message());
        if (raw.startsWith(CRITICAL_PREFIX)) {
            return new ChatResponse(CANNED_CRITICAL, true);
        }
        return new ChatResponse(raw.strip(), false);
    }
}
