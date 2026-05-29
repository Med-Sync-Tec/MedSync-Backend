# Requirements — Dictado de voz IA para SOAP (Backend)

**Fecha:** 28-05-2026

---

## Necesidad

El frontend envía un blob de audio (grabado con MediaRecorder) al backend. El backend debe:
1. Transcribir el audio con **Groq Whisper**.
2. Usar **Groq LLaMA** para extraer el contenido en los 7 campos del SOAP.
3. Devolver un JSON con los campos identificados.

---

## Requisitos funcionales (EARS)

| ID | Requisito |
|----|-----------|
| REQ-1 | El sistema **shall** exponer `POST /api/soap/dictation` protegido por Firebase Auth. |
| REQ-2 | El sistema **shall** aceptar `multipart/form-data` con un campo `audio` (archivo de audio webm/mp4/ogg/wav). |
| REQ-3 | El sistema **shall** enviar el audio a Groq Whisper (`whisper-large-v3`) para transcribir. |
| REQ-4 | El sistema **shall** enviar el transcript a Groq LLaMA con un prompt que extraiga los 7 campos del SOAP en JSON. |
| REQ-5 | El sistema **shall** devolver `{ motivoConsulta, subjetivo, objetivo, evaluacion, diagnostico, plan, prescripcion }` con solo los campos que la IA pudo inferir (los demás omitidos o null). |
| REQ-6 | El sistema **shall** rechazar requests sin campo `audio` con HTTP 400. |
| REQ-7 | El sistema **shall** estar accesible para el rol `DOCTOR`. |

---

## Requisitos no funcionales

| ID | Requisito |
|----|-----------|
| RNF-1 | Tamaño máximo de audio: 25 MB (límite de Groq Whisper). |
| RNF-2 | No se almacena el audio ni el transcript en base de datos. |
| RNF-3 | Tiempo de respuesta < 30 s (Whisper + LLaMA en Groq son rápidos). |

---

## Fuera de alcance

- Streaming de transcripción.
- Persistencia del audio.
- Soporte para múltiples idiomas (solo español).
