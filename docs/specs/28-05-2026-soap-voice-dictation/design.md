# Design — Dictado de voz IA para SOAP (Backend)

---

## Paquetes nuevos

```
domain/soapdictation/
  usecase/TranscribeAndExtractSOAPUseCase.java
  repository/WhisperGateway.java          ← puerto de dominio (Clean Architecture)
  model/SOAPDictationResult.java          ← record con los 7 campos (todos nullable)

application/soapdictation/
  TranscribeAndExtractSOAPService.java    ← solo importa domain/

infrastructure/ai/
  GroqWhisperGateway.java                 ← implementa WhisperGateway
                                             reutiliza GroqAiAnalysisGatewayConfig
                                             construye multipart manualmente

interfaces/rest/soapdictation/
  SOAPDictationResource.java              ← POST /api/soap/dictation
  SOAPDictationResponse.java              ← DTO de salida
```

> No hay entidad JPA ni migración Flyway — nada se persiste.

---

## Dependency Rule

```
domain/soapdictation/   ← cero imports externos
application/            ← solo importa domain/ (WhisperGateway + ChatGateway interfaces)
infrastructure/ai/      ← implementa WhisperGateway + ChatGateway
interfaces/rest/        ← importa domain/ + Jakarta REST
```

---

## Dominio

```java
// domain/soapdictation/model/SOAPDictationResult.java
public record SOAPDictationResult(
    String motivoConsulta,
    String subjetivo,
    String objetivo,
    String evaluacion,
    String diagnostico,
    String plan,
    String prescripcion
) {}

// domain/soapdictation/repository/WhisperGateway.java
public interface WhisperGateway {
    /** Transcribes audio bytes to text using Whisper. */
    String transcribe(byte[] audioBytes, String mimeType);
}

// domain/soapdictation/usecase/TranscribeAndExtractSOAPUseCase.java
public interface TranscribeAndExtractSOAPUseCase {
    SOAPDictationResult transcribeAndExtract(byte[] audioBytes, String mimeType);
}
```

---

## Gateway Groq Whisper — multipart manual

Java `HttpClient` no tiene soporte nativo para `multipart/form-data`. Hay que
construir el body y el boundary manualmente:

```java
@ApplicationScoped
public class GroqWhisperGateway implements WhisperGateway {

    // Endpoint: config.baseUrl() + "/audio/transcriptions"
    //   = "https://api.groq.com/openai/v1/audio/transcriptions"
    //
    // Multipart fields (Groq Whisper API):
    //   file        → audioBytes con filename="audio.webm" y Content-Type del mimeType
    //   model       → "whisper-large-v3"
    //   language    → "es"
    //   response_format → "text"  (devuelve texto plano, NO el envelope de choices[])
    //
    // Respuesta: texto plano (String), no JSON — NO usar GroqEnvelope para parsear
    //
    // HTTP/1.1 (igual que GroqChatGateway)
    // Reutiliza GroqAiAnalysisGatewayConfig (misma API key, mismo base URL)

    private byte[] buildMultipartBody(String boundary, byte[] audioBytes,
                                       String mimeType, String filename) {
        // Construye el cuerpo multipart con los 4 campos requeridos por Groq Whisper
    }

    @Override
    public String transcribe(byte[] audioBytes, String mimeType) {
        String boundary = "boundary_" + System.currentTimeMillis();
        byte[] body = buildMultipartBody(boundary, audioBytes, mimeType, "audio.webm");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/audio/transcriptions"))
            .timeout(config.timeout())
            .header("Authorization", "Bearer " + config.apiKey())
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();

        // response.body() es texto plano — leer directamente como String
        // En error HTTP → throw ChatException (reusar para errores de Groq)
    }
}
```

---

## `ChatGateway` — agregar `completeJson()`

La extracción de campos SOAP necesita JSON mode. Se agrega a la interfaz existente:

```java
// domain/chat/repository/ChatGateway.java
public interface ChatGateway {
    String complete(String systemPrompt, String userMessage);         // text mode
    String completeJson(String systemPrompt, String userMessage);     // JSON mode
}
```

`GroqChatGateway` implementa `completeJson()` igual que `complete()` pero
pasando `GroqResponseFormat.jsonObject()` en vez de `null`.

---

## Servicio `TranscribeAndExtractSOAPService`

```
1. WhisperGateway.transcribe(audioBytes, mimeType) → transcript (String plano)
2. ChatGateway.completeJson(SYSTEM_PROMPT, transcript) → jsonString
3. ObjectMapper.readTree(jsonString) → extraer los 7 campos
4. Devolver SOAPDictationResult (campos null si la IA no los infirió)
```

**Prompt de sistema para extracción:**
```
Eres un asistente médico. Dado el siguiente dictado en español, extrae y
clasifica la información en los campos del formato SOAP.

Responde ÚNICAMENTE con un JSON:
{
  "motivoConsulta": "...",
  "subjetivo": "...",
  "objetivo": "...",
  "evaluacion": "...",
  "diagnostico": "...",
  "plan": "...",
  "prescripcion": "..."
}

- Omite (null) los campos que no puedas inferir.
- No inventes información que no esté en el dictado.
- Responde en español.
```

---

## Endpoint REST

```
POST /api/soap/dictation
Authorization: Bearer <firebase-token>
Content-Type: multipart/form-data

[campo: audio] → archivo de audio (webm/mp4/ogg/wav, max 25 MB)

→ 200 OK
{
  "motivoConsulta": "Dolor abdominal de 3 días",
  "subjetivo": "Paciente refiere...",
  "objetivo": "Abdomen rígido...",
  "evaluacion": "Compatible con apendicitis",
  "diagnostico": "Apendicitis aguda",
  "plan": "Derivar a cirugía",
  "prescripcion": null
}
```

**Quarkus recibe el multipart:**
```java
@POST
@Path("/api/soap/dictation")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response dictate(@RestForm("audio") FileUpload audio) {
    byte[] bytes = Files.readAllBytes(audio.uploadedFile());
    String mimeType = audio.contentType();
    ...
}
```

---

## Secuencia

```
Client → SOAPDictationResource.dictate(audioFile)
  → bytes = audio.uploadedFile().readAllBytes()
  → TranscribeAndExtractSOAPService.transcribeAndExtract(bytes, mimeType)
    → WhisperGateway.transcribe(bytes, mimeType)       [multipart a Groq]
      ← transcript (String plano)
    → ChatGateway.completeJson(SYSTEM_PROMPT, transcript)  [JSON mode]
      ← jsonString
    ← SOAPDictationResult
  ← SOAPDictationResponse
← 200 { campos... }
```

---

## Tests

**`TranscribeAndExtractSOAPServiceTest`** (unit, Mockito):
- Mocks de `WhisperGateway` y `ChatGateway`
- Transcript con todos los campos → resultado completo
- Transcript parcial → campos no inferidos son null
- JSON inválido del LLaMA → excepción

**`GroqWhisperGatewayTest`** (unit, HttpServer local):
- Mismo patrón que `GroqChatGatewayTest`
- Verifica `Content-Type: multipart/form-data` con boundary
- Verifica que el body contiene `model=whisper-large-v3` y `language=es`
- 200 → devuelve transcript como String plano
- 401 → `ChatException`
