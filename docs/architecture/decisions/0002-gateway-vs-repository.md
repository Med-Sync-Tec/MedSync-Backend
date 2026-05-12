# ADR 0002: Gateway vs Repository Naming Convention

- **Status**: Accepted
- **Date**: 2026-04-20
- **Deciders**: Backend team

## Context

In Clean Architecture, all output ports live in `domain/<feature>/repository/`. But MedSync interacts with two kinds of external systems:

1. **Data that MedSync owns** — patients, users, articles, medication catalog. Stored in our own MySQL database. CRUD semantics.
2. **Data that someone else owns** — the hospital's clinical records (`expedientes_clinicos`, `consultas`). We read mostly, write rarely, and we do not control the schema.

Calling both "repositories" hides this distinction. A reader can't tell whether `XGateway.findX(id)` may fail with a connection timeout to a remote system, or whether it's a fast query against our own DB.

## Decision

We use two naming conventions for output ports:

- **`*Repository`** → owns data we control. Full CRUD. Lives in our database. Backed by `PanacheRepository`.
- **`*Gateway`** → integrates with an external system or service. Often read-only or restricted writes. Backed by a hand-written adapter (often using a different persistence unit or HTTP client).

Both interfaces live in `domain/<feature>/repository/` — the folder name reflects the Clean Architecture role (output port), not the implementation flavor.

Current examples:

| Interface              | Type       | Backing                                       |
|------------------------|------------|-----------------------------------------------|
| `PatientRepository`    | Repository | Own MySQL + Panache                           |
| `UserRepository`       | Repository | Own MySQL + Panache                           |
| `ArticleRepository`    | Repository | Own MySQL + Panache                           |
| `MedicamentoRepository`| Repository | Own MySQL + Panache                           |
| `HospitalGateway`      | Gateway    | External MySQL via `EntityManager`            |
| *(future)* `AiTagGateway` | Gateway | External AI service via HTTP                  |
| *(future)* `PubmedGateway` | Gateway | External PubMed E-utilities (currently a service, not an output port) |

## Consequences

### Positive

- The reader knows at a glance whether a call may cross the network boundary.
- We document SLOs, retry policies, and timeouts per gateway. Repositories share the same DB transaction guarantees.
- When the hospital integration grows to multiple external systems, the naming pattern scales (`HospitalGateway`, `PharmacyChainGateway`, etc.).

### Negative

- New convention to teach. Mitigated by this ADR and `conventions/naming.md`.

### Neutral

- Both end in different suffixes, so refactors that promote a repository to a gateway (or vice versa) require renaming. We treat this as a healthy signal — the change in semantics deserves a visible rename.
