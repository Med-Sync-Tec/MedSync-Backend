# Design — MediBot Chat (Backend)

---

## Paquetes nuevos

```
domain/chat/
  repository/ChatGateway.java           ← puerto (interfaz del dominio hacia Groq)
  usecase/ChatUseCase.java              ← interfaz del caso de uso
  model/ChatRequest.java                ← record (message: String)
  model/ChatResponse.java               ← record (response: String, isCritical: boolean)
  exception/ChatException.java          ← runtime exception para errores del gateway

application/chat/
  ChatService.java                      ← implementa ChatUseCase

infrastructure/ai/
  GroqChatGateway.java                  ← implementa ChatGateway; REUTILIZA
                                           GroqAiAnalysisGatewayConfig + GroqEnvelope

interfaces/rest/chat/
  ChatResource.java                     ← POST /api/chat
  ChatRequest.java                      ← DTO de entrada REST
  ChatResponseDto.java                  ← DTO de salida REST
```

> **No** crear paquete `infrastructure/groq/` — toda la infraestructura de Groq
> vive en `infrastructure/ai/` (ya existe `GroqAiAnalysisGateway` ahí).

---

## Dependency Rule (recordatorio)

```
domain/chat/       ← cero imports externos (sin Jakarta, sin Groq, sin HTTP)
application/chat/  ← solo importa domain/
infrastructure/ai/ ← importa domain/ + GroqEnvelope + GroqAiAnalysisGatewayConfig
interfaces/rest/   ← importa domain/ + Jakarta REST
```

---

## Dominio

```java
// domain/chat/model/ChatRequest.java
public record ChatRequest(String message) {}

// domain/chat/model/ChatResponse.java
public record ChatResponse(String response, boolean isCritical) {}

// domain/chat/exception/ChatException.java
public class ChatException extends RuntimeException {
    public ChatException(String msg) { super(msg); }
    public ChatException(String msg, Throwable cause) { super(msg, cause); }
}

// domain/chat/repository/ChatGateway.java
public interface ChatGateway {
    /** Sends a single user message and returns the assistant reply. */
    String complete(String systemPrompt, String userMessage);
}

// domain/chat/usecase/ChatUseCase.java
public interface ChatUseCase {
    ChatResponse chat(ChatRequest request);
}
```

---

## Servicio `ChatService`

```java
@ApplicationScoped
public class ChatService implements ChatUseCase {

    static final String CRITICAL_PREFIX = "CRITICAL:";

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

    static final String CANNED_CRITICAL =
        "Para situaciones de emergencia o decisiones clínicas críticas, " +
        "consulta los protocolos institucionales y al personal de guardia.";

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
```

---

## Gateway `GroqChatGateway`

```java
@ApplicationScoped
public class GroqChatGateway implements ChatGateway {

    // Reuses GroqAiAnalysisGatewayConfig (same ai.groq.* properties).
    // Reuses GroqEnvelope for request/response serialization.
    // Uses TEXT mode (no response_format), NOT json_object — chat is free-form.

    @Override
    public String complete(String systemPrompt, String userMessage) {
        GroqEnvelope.GroqChatRequest body = new GroqEnvelope.GroqChatRequest(
            config.model(),
            config.maxTokens(),
            0.7,       // slightly higher temperature for conversational tone
            null,      // response_format: null = text mode (NOT json_object)
            List.of(
                new GroqEnvelope.GroqMessage("system", systemPrompt),
                new GroqEnvelope.GroqMessage("user", userMessage)
            )
        );
        // HTTP call identical to GroqAiAnalysisGateway#callGroq(...)
        // On HTTP error or timeout → throw ChatException
        return choices[0].message.content;
    }
}
```

> **Importante:** revisar si `GroqEnvelope.GroqChatRequest` ya acepta `null`
> en el campo `response_format`. Si el campo es obligatorio en el record,
> habrá que hacerlo nullable (`@JsonInclude(NON_NULL)`).

---

## Endpoint REST

```
POST /api/chat
Authorization: Bearer <firebase-token>
Content-Type: application/json

{ "message": "¿Cuál es la dosis estándar de ibuprofeno en adultos?" }

→ 200 OK
{ "response": "La dosis estándar de ibuprofeno en adultos es...", "isCritical": false }
```

**Validaciones en el DTO:**
- `@NotBlank` en `message`
- `@Size(max = 1000)` en `message`
- HTTP 400 si falla (manejado por `GlobalExceptionHandler` existente — no se necesita mapper nuevo)

---

## Configuración

Reutilizar las properties existentes:

```properties
# ya existen en application.properties — no agregar nada nuevo
ai.groq.api-key=${GROQ_API_KEY:unconfigured}
ai.groq.model=llama-3.3-70b-versatile
ai.groq.max-tokens=2048
ai.groq.timeout=30s
ai.groq.base-url=https://api.groq.com/openai/v1
```

---

## Secuencia

```
Client → ChatResource.chat(dto)
  → ChatService.chat(ChatRequest)
    → GroqChatGateway.complete(SYSTEM_PROMPT, message)
      → Groq API POST /chat/completions (text mode)
      ← choices[0].message.content (string libre)
    ← rawText
  ← ChatResponse(response, isCritical)
← 200 { response, isCritical }
```

---

## Tests

**`ChatServiceTest`** (unit, sin Quarkus):
- `ChatGateway` mockeado con Mockito
- Caso normal → `isCritical=false`, texto devuelto limpio
- Respuesta que empieza con `"CRITICAL:"` → `isCritical=true`, mensaje canned
- Respuesta `"CRITICAL: algo más"` → también `isCritical=true`

**`GroqChatGatewayTest`** (unit, sin Quarkus):
- Mismo patrón que `GroqAiAnalysisGatewayTest`: levantar un `HttpServer` local,
  encolar respuestas, verificar body enviado y parsing de respuesta
- Caso happy: 200 con contenido → devuelve el string del content
- HTTP 401 → `ChatException` con mención a la API key
- HTTP 500 → `ChatException`
- Timeout → `ChatException` (o reusar `AiAnalysisTimeoutException` si se considera conveniente)
- Verificar que `response_format` es `null` en el body enviado (text mode)
