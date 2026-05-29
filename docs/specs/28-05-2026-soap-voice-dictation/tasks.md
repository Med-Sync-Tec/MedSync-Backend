# Tasks — Dictado de voz IA para SOAP (Backend)

**Approach:** TDD — test antes del código.

---

## Dominio

- [ ] **T-1** Crear `domain/soapdictation/model/SOAPDictationResult.java` (record, 7 campos nullable).
- [ ] **T-2** Crear `domain/soapdictation/repository/WhisperGateway.java` (interfaz de dominio — Clean Architecture).
- [ ] **T-3** Crear `domain/soapdictation/usecase/TranscribeAndExtractSOAPUseCase.java` (interfaz).

---

## Gateway Groq Whisper (TDD)

- [ ] **T-3** Escribir `GroqWhisperGatewayTest` en `src/test/.../infrastructure/ai/`:
  - Usar `HttpServer` local (mismo patrón que `GroqChatGatewayTest`)
  - 200 → devuelve transcript string
  - Verifica que el request es `multipart/form-data` con campo `file`
  - Verifica `model=whisper-large-v3` y `language=es` en el body
  - 401 → `ChatException` con mención a API key
  - 500 → `ChatException`
- [ ] **T-4** Crear `infrastructure/ai/GroqWhisperGateway.java` hasta que pasen los tests.
  - Reutiliza `GroqAiAnalysisGatewayConfig`
  - HTTP/1.1 (igual que `GroqChatGateway`)
  - Endpoint: `config.baseUrl() + "/audio/transcriptions"`

---

## Soporte JSON mode en `GroqChatGateway` (TDD)

- [ ] **T-5** Agregar test en `GroqChatGatewayTest`:
  - `completeJson(systemPrompt, userMessage)` incluye `response_format: {type: "json_object"}` en el body
- [ ] **T-6** Agregar método `completeJson(String systemPrompt, String userMessage)` en `GroqChatGateway` y en `ChatGateway` (interfaz del dominio).
  - Reutiliza el mismo código de `complete()` pero pasa `GroqResponseFormat.jsonObject()`

---

## Servicio (TDD)

- [ ] **T-7** Escribir `TranscribeAndExtractSOAPServiceTest`:
  - Mock de `GroqWhisperGateway` y `ChatGateway` con Mockito
  - Transcript completo → todos los campos mapeados correctamente
  - Transcript parcial → campos no mencionados son null
  - JSON inválido del LLaMA → lanza excepción
  - Whisper falla → excepción se propaga
- [ ] **T-8** Crear `application/soapdictation/TranscribeAndExtractSOAPService.java` hasta que pasen los tests.
  - Paso 1: `GroqWhisperGateway.transcribe()` → transcript
  - Paso 2: `ChatGateway.completeJson()` con prompt de extracción → JSON string
  - Paso 3: parsear JSON → `SOAPDictationResult`

---

## Endpoint REST (TDD)

- [ ] **T-9** Escribir `SOAPDictationResourceTest`:
  - `POST /api/soap/dictation` con archivo → 200 con los campos
  - `POST /api/soap/dictation` sin campo `audio` → 400
  - Mock de `TranscribeAndExtractSOAPUseCase`
- [ ] **T-10** Crear `interfaces/rest/soapdictation/SOAPDictationResponse.java` (DTO).
- [ ] **T-11** Crear `interfaces/rest/soapdictation/SOAPDictationResource.java`.

---

## QA manual

- [ ] **T-12** Smoke test con Postman: enviar audio de prueba y verificar los 7 campos en la respuesta.
- [ ] **T-13** Verificar que sin token devuelve 401.

---

## Post-ship

- [ ] **T-14** Llenar `summary.md`.
