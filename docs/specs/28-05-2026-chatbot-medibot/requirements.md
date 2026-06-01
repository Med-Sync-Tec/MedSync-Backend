# Requirements — MediBot Chat (Backend)

**Tickets:** EQ3005B-83, 84, 85, 86, 87  
**Fecha:** 28-05-2026

---

## Necesidad

El frontend necesita un endpoint que reciba la pregunta del profesional, la envíe a un modelo de lenguaje (Groq / Llama 3.3 — mismo proveedor usado para análisis de artículos), y devuelva la respuesta. El endpoint debe asegurarse de que solo se procesen preguntas no críticas.

---

## Requisitos funcionales (EARS)

| ID | Requisito |
|----|-----------|
| REQ-1 | El sistema **shall** exponer `POST /api/chat` protegido por Firebase Auth. |
| REQ-2 | El sistema **shall** aceptar un cuerpo JSON `{ "message": "<texto>" }`. |
| REQ-3 | El sistema **shall** enviar el mensaje a Groq con un prompt de sistema que restrinja las respuestas a preguntas médicas no críticas. |
| REQ-4 | **When** el modelo detecte que la pregunta implica una emergencia o decisión clínica crítica, **the system shall** responder con un mensaje canned estándar en lugar de la respuesta del modelo. |
| REQ-5 | El sistema **shall** devolver `{ "response": "<texto>", "isCritical": <boolean> }`. |
| REQ-6 | El sistema **shall** rechazar mensajes vacíos o mayores a 1 000 caracteres con HTTP 400. |
| REQ-7 | El sistema **shall** estar accesible para roles `DOCTOR` y `COO`. |

---

## Requisitos no funcionales

| ID | Requisito |
|----|-----------|
| RNF-1 | Tiempo de respuesta < 10 s en condiciones normales de red. |
| RNF-2 | No se almacena ningún mensaje ni respuesta en base de datos. |
| RNF-3 | No se envían datos identificables del paciente a Groq. |

---

## Fuera de alcance

- Historial de conversación persistido.
- Streaming SSE (iteración futura).
- Contexto de paciente en el prompt.
