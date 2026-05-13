# Feature: Consulta AI Analysis — Requirements

## Overview

A doctor-triggered endpoint that reads a consultation from the hospital database, joins its SOAP fields (motivo, subjetivo, objetivo, evaluación, plan, prescripción, diagnóstico) into a single text blob, and uses **Groq Llama 3.3 70B Versatile** (via the shared `AiAnalysisGateway` from feature 3) to extract a constrained list of `(tipo, valor)` clinical-context suggestions drawn from **the authenticated doctor's specialty vocabulary**.

Unlike the article flow, this endpoint **does not persist** the AI output. It returns the suggestions, the frontend shows them to the doctor for confirmation, and the doctor selects which ones to accept. A separate bulk endpoint (`POST /api/patients/{patientId}/contextos/bulk`, added to the existing `patient-context` feature) persists the accepted subset. Every persisted `paciente_contexto` row inherits the doctor's specialty (same rule introduced in feature 1).

## User stories

- As a **doctor**, after writing or reviewing a consultation, I want to ask the AI to suggest clinical-context tags for it, so that I do not have to manually transcribe `(tipo, valor)` pairs from my own notes.
- As a **doctor**, I want to review the AI's suggestions and accept only the ones I agree with, so that the patient's clinical context reflects my judgment, not the LLM's.
- As a **doctor**, I want accepted suggestions to be persisted in one round-trip, so that the UX is one action ("accept selected") instead of N.
- As a **system**, I want every persisted suggestion to be scoped to the inviting doctor's specialty, so that AI-extracted context interoperates with the article matching query without manual specialty assignment.

## Acceptance criteria (EARS format)

### Ubiquitous

- The system shall expose `POST /api/consultas/{consultaId}/analyze` to any authenticated user with an assigned specialty.
- The system shall expose `POST /api/patients/{patientId}/contextos/bulk` to any authenticated user (cross-feature addition to `patient-context`).
- The system shall use the same `AiAnalysisGateway` (Groq Llama 3.3 70B Versatile via raw HTTP, OpenAI-compatible API) introduced by feature 3 — no separate gateway, no separate model configuration.
- The system shall scope vocabulary selection by the **authenticated user's** `User.especialidadId` (the original consulta doctor recorded in the hospital DB is **not** consulted).
- The system shall **not** persist any data from the analyze endpoint — the response is purely suggestions for the frontend to display.
- The system shall persist accepted entries in a single transaction at the bulk endpoint — all entries succeed together or none do.
- The system shall inherit the **authenticated user's** specialty on every row persisted by the bulk endpoint.

### Event-driven

- When a `POST /api/consultas/{consultaId}/analyze` request arrives from an authenticated user whose `User.especialidadId` is set, the system shall:
  1. Resolve the consulta via `HospitalGateway.findConsultaById(consultaId)`.
  2. Join SOAP and adjunct fields (`motivoConsulta`, `subjetivo`, `objetivo`, `evaluacion`, `plan`, `prescripcion`, `diagnostico`) into a single text blob using a deterministic format documented in `design.md`.
  3. Load the doctor's specialty vocabulary via `VocabularyRepository.getVocabularyFor(User.especialidadId)`.
  4. Call `AiAnalysisGateway.analyzeConsultaText(...)`, receiving `List<ExtractedTag>`.
  5. Return `200 OK` with the suggestions (no persistence).
- When a `POST /api/patients/{patientId}/contextos/bulk` request arrives from an authenticated user with a valid payload, the system shall:
  1. Verify the patient exists and is active.
  2. For each entry, validate format (`tipo` is a known `TipoClinico` value, `valor` is 1..500 non-blank).
  3. Construct one `PacienteContexto` per entry with `especialidadId = caller.user.especialidadId` (may be `null` if the caller has no specialty).
  4. Persist them all atomically and return `201 Created` with the list of persisted entries.

### Conditional (`/analyze`)

- If the path `{consultaId}` does not resolve in the hospital DB, the system shall return `404 Not Found` (`ConsultaNotFoundException` — already mapped).
- If the joined consulta text is blank (all SOAP and adjunct fields are null or empty), the system shall return `400 Bad Request` (`InvalidConsultaDataException`, new) with a message naming the consulta id.
- If the authenticated user has no `especialidadId`, the system shall return `400 Bad Request` (`UserHasNoSpecialtyException`, new) — the doctor must have a specialty before the AI flow can choose a vocabulary.
- If the doctor's specialty exists but has an **empty** vocabulary (no JSON file or all four `TipoClinico` buckets empty), the system shall return `200 OK` with `suggestions: []` and a clear `vocabularyStatus: "empty"` flag in the response — this is not an error, it is a degraded mode.
- If the Groq API returns malformed output, the system shall return `502 Bad Gateway` (reuses `AiAnalysisException` from feature 3).
- If the Groq API call exceeds the configured timeout, the system shall return `504 Gateway Timeout` (reuses `AiAnalysisTimeoutException`).
- If unauthenticated, the system shall return `401 Unauthorized`.

### Conditional (`/contextos/bulk`)

- If `{patientId}` does not resolve to an active patient, the system shall return `404 Not Found` (`PatientNotFoundException` — already mapped).
- If the request body is missing or `entries` is empty, the system shall return `400 Bad Request`.
- If any entry fails format validation (unknown `tipo`, blank `valor`, `valor` > 500 chars), the system shall return `400 Bad Request` and **persist nothing** (all-or-nothing semantics).
- If a duplicate entry is submitted (same `(tipo, valor)` already present for this patient), the system shall **still persist it** — the patient-context feature does not dedupe at the existing single-entry endpoint either, and consistency with that behavior matters more than dedup convenience. This is documented behavior, not a TODO.
- If unauthenticated, the system shall return `401 Unauthorized`.

## Non-functional requirements

- **Latency**: P95 analyze call ≤ 6 seconds (one Groq round-trip; typical latency is 1–3 s).
- **Cost**: each analyze call consumes one Llama 3.3 70B input window (consulta text + vocabulary slice). At the project's scale this stays inside Groq's free-tier quota (~12k tokens/min on `llama-3.3-70b-versatile` at the time of writing); a single consulta is comfortably under 4k tokens combined.
- **No data leaves the boundary except via the gateway**: the consulta text contains PHI; we send it to Groq Cloud. This is the same trust boundary as feature 3's abstract-sending and is documented in the AI vendor agreement, not re-litigated here.
- **Test coverage**: ≥ 80% on `domain/consultaaianalysis` and `application/consultaaianalysis`. The bulk-endpoint additions to `patient-context` are covered by additions to the existing patient-context test suites.

## Out of scope (explicit)

- **Auto-save of consulta suggestions**. The review-then-save flow is the explicit design — feature 3 is the auto-save flow.
- **Selecting a vocabulary different from the doctor's specialty**. The doctor cannot ask "show me oncology suggestions for this consulta" — the specialty is taken from `AuthenticatedUserContext`.
- **Editing accepted suggestions via the bulk endpoint**. Bulk creates only. Edits use the existing single-entry endpoint or are out of scope.
- **Deleting via bulk**. Bulk deletes are not implemented.
- **Re-analyzing a consulta to compare suggestions over time**. Each invocation is a fresh call to the LLM; we do not store prior analyses.
- **Tag-source tracking** (which `paciente_contexto` rows came from AI vs which were manually added). Not tracked.
- **Cross-doctor specialty inheritance**. Even if doctor A originally wrote the consulta and doctor B (different specialty) analyzes it, the inherited specialty is **doctor B's**. We surface this in the design's "Key technical decisions" so future readers do not mistake it for a bug.
- **Hospital write-back**. The bulk endpoint writes to MedSync's `paciente_contexto`, never to the hospital's `consultas` table.

## Open questions

- Should the analyze response include a `confidence` score per suggestion? *Not blocking — `llama-3.3-70b-versatile` does not natively return calibrated confidences without further prompting. Revisit if doctors ask for it.*
- Should we cap consulta text length sent to Groq to avoid token blowouts on very long records? *Yes — truncate at 50,000 chars (the upper-bound of a verbose multi-page consulta) and log a warning. Detail in design.md.*
