# Feature: Hospital — Requirements

## Overview

Integration with the **external hospital database**. MedSync reads clinical records (`expedientes_clinicos`) and SOAP-format consultations (`consultas`) from a separate MySQL instance that the hospital owns. MedSync may also create new `consulta` rows (and auto-create an `expediente_clinico` if the patient has none). This feature establishes the **Gateway pattern** for all future external-system integrations, distinguishing them from `*Repository` which only handles MedSync-owned data.

## User stories

- As a **doctor**, I want to view a patient's external clinical chart, so that I have full context before prescribing.
- As a **doctor**, I want to list a patient's previous consultations, so that I can review their history.
- As a **doctor**, I want to view the details of a single past consultation, so that I can read the SOAP notes.
- As a **doctor**, I want to record a new consultation, so that today's encounter is captured — without having to first create an `expediente` if the patient has none.

## Acceptance criteria (EARS format)

### Ubiquitous
- The system shall connect to the hospital database via a separate Quarkus datasource named `hospital`.
- The system shall not modify the hospital schema (Hibernate `validate` mode).
- The system shall use SOAP-shaped fields (`subjetivo`, `objetivo`, `evaluacion`, `plan`) plus `prescripcion`, `diagnostico`, `motivoConsulta`, and `fecha` for consultas.

### Event-driven
- When a get-expediente request is received for an existing patient with an existing chart, the system shall return `200 OK` with the expediente.
- When a list-consultas request is received for an existing patient, the system shall return `200 OK` with consultas ordered by `fecha DESC`.
- When a get-consulta-by-id request is received for an existing consulta, the system shall return `200 OK` with the consulta.
- When a create-consulta request is received for an existing patient who has an `expediente`, the system shall return `201 Created` with the new consulta.
- When a create-consulta request is received for an existing patient who has no `expediente`, the system shall auto-create one (using only `pacienteExternoId`) and proceed.

### Conditional
- If the path-parameter patient id is not found in MedSync, the system shall return `404`.
- If the patient exists but has no expediente (on a GET-expediente request), the system shall return `404 ExpedienteNotFoundException`.
- If the patient exists but has no consultas, the system shall return `200` with an empty list (not 404).
- If a get-consulta references a non-existent id, the system shall return `404 ConsultaNotFoundException`.
- If `fecha` is missing from the create-consulta request, the system shall return `400`.

### State-driven
- While the hospital DB is unreachable, the system shall surface a generic `500` (mapping to `503` is acknowledged as future work).

## Non-functional requirements

- **Data isolation**: cross-DB writes never need atomicity (no XA).
- **Local dev**: developer must be able to run the feature without a live hospital — H2 with `drop-and-create` regenerates the hospital schema from `@Entity` classes in `%dev` and `%test`.
- **Container parity**: the `mysql-local` profile uses Docker-managed MySQL on port 3308, mirroring prod topology.

## Out of scope (explicit)

- Edit (`PUT`/`PATCH`) consultas.
- Delete consultas.
- Explicit endpoint to create an `expediente` (auto-create is the only path).
- Authentication / authorization on these endpoints (handled separately).
- Real-time sync (webhooks, polling).
- Caching hospital responses.
- Retries on connection failure.
- Pagination / filters on the consultas list.

## Open questions

- Should auto-creation of `expediente_clinico` require approval from the hospital's policy layer? *Documented as a risk in 1-05-2026/design.md and 1-05-2026/summary.md.*
