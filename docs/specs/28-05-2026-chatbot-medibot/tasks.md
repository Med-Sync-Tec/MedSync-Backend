# Tasks — MediBot Chat (Backend)

**Approach:** Test-Driven Development — escribir el test antes de la implementación.

---

## Dominio

- [ ] **T-1** Crear `domain/chat/model/ChatRequest.java` y `ChatResponse.java` (records).
- [ ] **T-2** Crear `domain/chat/exception/ChatException.java`.
- [ ] **T-3** Crear `domain/chat/repository/ChatGateway.java` (interfaz).
- [ ] **T-4** Crear `domain/chat/usecase/ChatUseCase.java` (interfaz).

---

## Servicio (TDD)

- [ ] **T-5** Escribir `ChatServiceTest` en `src/test/java/.../application/chat/`:
  - Mock de `ChatGateway` con Mockito
  - Respuesta normal → `isCritical=false`, texto correcto
  - Respuesta que empieza con `"CRITICAL:"` → `isCritical=true`, mensaje canned exacto
  - Texto con `"CRITICAL:"` en el medio (no al inicio) → `isCritical=false`
- [ ] **T-6** Crear `application/chat/ChatService.java` hasta que pasen todos los tests de T-5.

---

## Gateway Groq (TDD)

- [ ] **T-7** Revisar `GroqEnvelope.GroqChatRequest`: verificar si el campo `response_format`
  admite `null`. Si no, hacerlo nullable con `@JsonInclude(JsonInclude.Include.NON_NULL)`.
- [ ] **T-8** Escribir `GroqChatGatewayTest` en `src/test/java/.../infrastructure/ai/`:
  - Usar el mismo patrón que `GroqAiAnalysisGatewayTest` (levantar `HttpServer` local)
  - 200 con content → devuelve el string correctamente
  - `response_format` es **ausente** en el body enviado (text mode)
  - HTTP 401 → `ChatException` con mención a API key
  - HTTP 500 → `ChatException`
  - Timeout → `ChatException`
- [ ] **T-9** Crear `infrastructure/ai/GroqChatGateway.java` hasta que pasen todos los tests de T-8.
  - Inyectar `GroqAiAnalysisGatewayConfig` (reusar — mismas properties `ai.groq.*`)
  - Reusar `GroqEnvelope` para serialización/deserialización
  - Temperature: `0.7` (conversacional, no `0.0` como en análisis de artículos)

---

## Endpoint REST (TDD)

- [ ] **T-10** Escribir `ChatResourceTest` en `src/test/java/.../interfaces/rest/chat/`:
  - Mock de `ChatUseCase` con Mockito o `@InjectMock` de Quarkus
  - `POST /api/chat` con body válido → 200 con `response` e `isCritical`
  - `POST /api/chat` con `message` vacío → 400
  - `POST /api/chat` con `message` de 1001 chars → 400
  - Sin token → 401 (manejado por el filtro, probar si aplica en tests de resource)
- [ ] **T-11** Crear `interfaces/rest/chat/ChatRequest.java` (DTO con `@NotBlank` + `@Size(max=1000)`).
- [ ] **T-12** Crear `interfaces/rest/chat/ChatResponseDto.java`.
- [ ] **T-13** Crear `interfaces/rest/chat/ChatResource.java` con `POST /api/chat` hasta que pasen los tests de T-10.

---

## QA manual

- [ ] **T-14** Smoke test con Swagger UI (`/q/swagger-ui`): pregunta normal y pregunta crítica.
- [ ] **T-15** Verificar que sin token devuelve 401.
- [ ] **T-16** Verificar que `message` con 1001 caracteres devuelve 400.

---

## Post-ship

- [ ] **T-17** Llenar `summary.md`.
