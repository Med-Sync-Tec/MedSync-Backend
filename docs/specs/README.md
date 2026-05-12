# Feature Specifications

One folder per feature. Inside each feature folder lives one **dated snapshot folder per spec revision**, named `DD-MM-YYYY`. Inside the dated folder live the four lifecycle files:

```
specs/
└── <feature>/
    └── <DD-MM-YYYY>/           ← one folder per revision
        ├── requirements.md
        ├── design.md
        ├── tasks.md
        └── summary.md
```

Written in this order:

1. **`requirements.md`** — what the feature must do. Written before any design or code.
2. **`design.md`** — how we'll build it: domain model, persistence, endpoints, sequence flows.
3. **`tasks.md`** — ordered, actionable checklist. One item ≈ one commit.
4. **`summary.md`** — written **after** implementation. What actually shipped, deviations, deferred items.

## Why dated folders

Specs evolve. A feature's `1-05-2026/` snapshot captures what we agreed to build on May 1, 2026. When the same feature is reworked later, create `15-09-2026/` (or whatever date) next to it. Old snapshots stay — they document what we used to think, which is the only honest record of how the system grew.

The feature folder always points to the **latest** snapshot (no symlink — just pick the most recent date when reading). PRs that modify a spec start a new snapshot rather than overwriting an old one.

## Spec lifecycle

```
requirements.md   ─►  human review  ─►  design.md   ─►  human review  ─►  tasks.md
                                                                              │
                                                                              ▼
                                                                       (implementation)
                                                                              │
                                                                              ▼
                                                                        summary.md
```

Do not jump ahead. Do not write `design.md` until `requirements.md` is reviewed. Do not write code until `tasks.md` is reviewed.

## Feature index

The link points to the latest snapshot.

| Feature           | Status      | Latest snapshot                                                |
|-------------------|-------------|----------------------------------------------------------------|
| `patient`         | Implemented | [patient/1-05-2026/](patient/1-05-2026/)                       |
| `hospital`        | Implemented | [hospital/1-05-2026/](hospital/1-05-2026/)                     |
| `user`            | Implemented | [user/1-05-2026/](user/1-05-2026/)                             |
| `article`         | Implemented | [article/1-05-2026/](article/1-05-2026/)                       |
| `medication`      | Implemented | [medication/1-05-2026/](medication/1-05-2026/)                 |
| `patient-context` | Implemented | [patient-context/1-05-2026/](patient-context/1-05-2026/)       |
| `alert`           | Planned     | *(spec not yet written)*                                       |

## Templates

### `requirements.md` template

```markdown
# Feature: <Name>

## Overview

One paragraph: what the feature does and why it exists.

## User stories

- As a <role>, I want <capability>, so that <value>.
- As a <role>, I want <capability>, so that <value>.

## Acceptance criteria (EARS format)

EARS = Easy Approach to Requirements Syntax. Each criterion starts with one of:
- **When** <event/trigger>, the system shall <response>.
- **While** <state>, the system shall <response>.
- **If** <condition>, then the system shall <response>.
- **Where** <location/context>, the system shall <response>.

### Ubiquitous (always true)
- The system shall ...

### Event-driven
- When a doctor submits a valid create-patient request, the system shall persist the patient and return 201 with the created resource.

### State-driven
- While a patient is soft-deleted, the system shall hide it from list endpoints.

### Conditional
- If the request payload fails validation, then the system shall return 400 with field-level error details.

## Non-functional requirements

- **Performance**: P95 list endpoint ≤ 200ms with 10k rows.
- **Security**: only authenticated users with role X can ...
- **Observability**: log every state transition.
- ...

## Out of scope (explicit)

- Editing patients (`PUT` / `PATCH`).
- Bulk import.

## Open questions

- [ ] Should soft-deleted patients be restorable? *(decision pending product input)*
```

### `design.md` template

```markdown
# Feature: <Name> — Design

## Domain model

`<Feature>` (immutable POJO, lives in `domain/<feature>/model/`):

| Field             | Type           | Required | Notes                              |
|-------------------|----------------|----------|------------------------------------|
| id                | UUID           | yes      | generated server-side              |
| ...               | ...            | ...      | ...                                |

### Invariants (validated in constructor)
- ...

### Value objects
- ...

## Output ports (interfaces in `domain/<feature>/repository/`)

- `<Feature>Repository` (own data) — methods: ...
- *(or)* `<System>Gateway` (external) — methods: ...

## Use cases

| Use case interface             | Service implementation         | Description |
|--------------------------------|--------------------------------|-------------|
| `Create<Feature>UseCase`       | `Create<Feature>Service`       | ...         |
| `Get<Feature>ByIdUseCase`      | `Get<Feature>ByIdService`      | ...         |

## REST endpoints

| Method | Path                          | Request DTO            | Response DTO         | Status codes |
|--------|-------------------------------|------------------------|----------------------|--------------|
| POST   | `/api/<features>`             | `Create<Feature>Request` | `<Feature>Response` | 201 / 400 / 409 |
| GET    | `/api/<features>/{id}`        | —                      | `<Feature>Response`  | 200 / 404   |

## Persistence

### Entity: `<Feature>Entity`

| Column            | Type           | Constraints                       |
|-------------------|----------------|-----------------------------------|
| id                | `BINARY(16)`   | PK                                |
| ...               | ...            | ...                               |

### Migration: `V<N>__<description>.sql`

```sql
CREATE TABLE ...
```

### `@NamedEntityGraph` definitions
- `<Feature>.withX` — eager loads X.

## Validation

- Format (DTO): list of `@Size`, `@NotBlank`, `@Email`, etc.
- Business (domain constructor): list of invariants.
- Application-level (service): uniqueness, cross-entity references.

## Exceptions

| Domain exception                | HTTP code |
|---------------------------------|-----------|
| `<Feature>NotFoundException`    | 404       |
| `Duplicate<Feature>Exception`   | 409       |
| `Invalid<Feature>DataException` | 400       |

## Sequence flows

### Create

```
Client          PatientResource     CreatePatientService     PatientRepository      DB
  │  POST /api/patients  │                  │                       │                │
  │ ─────────────────────►                  │                       │                │
  │                      │ execute(...)     │                       │                │
  │                      │ ─────────────────►                       │                │
  │                      │                  │ existsByExpedienteX(..) │              │
  │                      │                  │ ──────────────────────►                │
  │                      │                  │                       │ SELECT COUNT   │
  │                      │                  │                       │ ──────────────►│
  │                      │                  │ Patient ← new Patient │                │
  │                      │                  │ save(patient)         │                │
  │                      │                  │ ──────────────────────►                │
  │                      │                  │                       │ INSERT         │
  │                      │ ◄─── Patient ────│                       │                │
  │ ◄─── 201 + body ─────│                  │                       │                │
```

## Open technical decisions / risks

- ...
```

### `tasks.md` template

```markdown
# Feature: <Name> — Implementation Tasks

Execute in order. Each item ≈ one commit. Use TDD per `conventions/testing.md`.

## Domain
- [ ] Write `<Feature>Test` (unit, JUnit pure) covering all invariants → RED
- [ ] Implement `<Feature>` model + `Invalid<Feature>DataException` → GREEN
- [ ] Define `<Feature>Repository` (or `<System>Gateway`) interface
- [ ] Define `<Action><Feature>UseCase` interfaces (one per use case)

## Application
- [ ] Write `<Action><Feature>ServiceTest` (Mockito) → RED
- [ ] Implement `<Action><Feature>Service` → GREEN

## Persistence
- [ ] Write Flyway migration `V<N>__<description>.sql`
- [ ] Write `<Feature>RepositoryImplTest` (`@QuarkusTest` + `@TestTransaction`) → RED
- [ ] Implement `<Feature>Entity` + `<Feature>PersistenceMapper` + `<Feature>RepositoryImpl` → GREEN
- [ ] Verify Hibernate `validate` boots cleanly

## REST
- [ ] Write `<Feature>ResourceIT` (`@QuarkusTest` + RestAssured) covering all endpoints + status codes → RED
- [ ] Implement DTOs (`<Action><Feature>Request`, `<Feature>Response`) + `<Feature>RestMapper` + `<Feature>Resource` → GREEN
- [ ] Add exceptions to `GlobalExceptionHandler` with correct HTTP codes
- [ ] Verify Swagger UI lists the new endpoints under the right `@Tag`

## Verification
- [ ] `./mvnw compile` clean
- [ ] `./mvnw test` all green
- [ ] `./mvnw verify -DskipITs=false` all green
- [ ] Coverage ≥80% on `domain/<feature>` and `application/<feature>`
- [ ] Manual smoke test via Swagger UI

## Wrap-up
- [ ] Write `summary.md` describing what shipped and any deviations from `design.md`
```

### `summary.md` template

```markdown
# Feature: <Name> — Implementation Summary

## What shipped

One paragraph summary of the delivered feature.

## Endpoints delivered

(Same table as design.md — but reflect what was actually built.)

## Deviations from design

- We changed X because Y. Original design said Z.

## Deferred from scope

- We had planned A but cut it because B. Tracked here: ...

## Acknowledged technical debt

- ...

## Tests

| File                          | Type             | What it covers      |
|-------------------------------|------------------|---------------------|
| `<Feature>Test`               | unit             | ...                 |
| `<Feature>RepositoryImplTest` | integration      | ...                 |
| `<Feature>ResourceIT`         | REST integration | ...                 |

## Next features that depend on this one

- ...
```
