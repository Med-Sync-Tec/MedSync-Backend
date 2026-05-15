# Architecture Overview

MedSync Backend follows **Clean Architecture** with a **Layer-by-Feature** organization. The system has four layers and one feature folder per business capability.

## The four layers

```
src/main/java/itesm/medsync/
│
├── domain/             Pure business rules. Zero external dependencies.
│   └── <feature>/
│       ├── model/         Immutable POJOs (no annotations from frameworks)
│       ├── usecase/       Use case interfaces (one per operation)
│       ├── repository/    Port interfaces: *Repository (own data), *Gateway (external)
│       └── exception/     Business exceptions
│
├── application/        Use case implementations (orchestration only).
│   └── <feature>/
│       └── <Action>Service.java   Implements one usecase interface
│
├── infrastructure/     Concrete adapters and configuration.
│   ├── persistence/<feature>/    JPA entities, Panache repositories, mappers
│   ├── hospital/                 Gateway to external hospital DB
│   ├── pubmed/                   PubMed E-utilities client + scheduler
│   ├── ai/                       (reserved) AI service gateway for tag extraction
│   ├── security/                 Auth filters, JWT, password hashing
│   └── config/                   Exception handlers, CORS, datasources
│
└── interfaces/         Entry points to the system.
    └── rest/<feature>/   JAX-RS resources, request/response DTOs, REST mappers
```

## Dependency rule (CRITICAL)

```
domain/          ← imports nothing external (no JPA, no Quarkus, no Jakarta)
application/     ← imports only domain/
infrastructure/  ← imports domain/ + frameworks (JPA, Panache, HTTP clients)
interfaces/      ← imports domain/ + Jakarta REST
```

If you see `jakarta.persistence`, `io.quarkus.*`, or `jakarta.ws.rs` in `domain/` or `application/` — **that is a bug**. Fix it before merging.

`interfaces` and `infrastructure` never import each other. They communicate only through `domain` ports.

## Why layer-by-feature, not layer-by-type

Traditional Clean Architecture organizes by layer (`controllers/`, `services/`, `repositories/`), forcing you to jump across the codebase to understand one capability. Layer-by-feature inverts that: all code for `patient` lives under `*/patient/`, all code for `article` lives under `*/article/`. Cohesion goes up, coupling goes down.

The trade-off: a new feature requires creating four folders instead of one. That cost is paid once per feature and saves hours during maintenance.

## One service per use case

Each `*UseCase` interface in `domain/` has exactly **one** implementation class in `application/`. We never write a "ArticleService" that implements `CreateArticleUseCase`, `GetArticleByIdUseCase`, and `ListArticlesUseCase` at once.

Why: single responsibility, easier mocking in tests, smaller diffs in PRs. A `CreateArticleService` is 30 lines. A god-service grows to 800.

## Where requests flow

```
HTTP request
   │
   ▼
┌──────────────────────────────────────────────────┐
│ interfaces/rest/<feature>/<Feature>Resource      │
│   • JAX-RS endpoint                              │
│   • Receives Request DTO (Jakarta validation)    │
│   • Calls *RestMapper to extract primitives      │
│   • Invokes *UseCase                             │
│   • Returns Response DTO                         │
└────────────────────────┬─────────────────────────┘
                         ▼
┌──────────────────────────────────────────────────┐
│ application/<feature>/<Action>Service            │
│   • Implements <Action>UseCase                   │
│   • Orchestrates: validation → domain → port     │
│   • No business logic of its own                 │
└────────────────────────┬─────────────────────────┘
                         ▼
┌──────────────────────────────────────────────────┐
│ domain/<feature>/                                │
│   • Model invariants enforced in constructor     │
│   • Port (*Repository / *Gateway) called via DI  │
└────────────────────────▲─────────────────────────┘
                         │ implements
┌────────────────────────┴─────────────────────────┐
│ infrastructure/persistence/<feature>/            │
│   • <Feature>Entity (@Entity)                    │
│   • <Feature>RepositoryImpl                      │
│     implements <Repository> + PanacheRepository  │
│   • <Feature>PersistenceMapper (Entity ↔ Domain) │
└──────────────────────────────────────────────────┘
```

## Implemented features

| Feature              | Package name (current) | Purpose                                                          | Latest spec snapshot                                                           |
|----------------------|------------------------|------------------------------------------------------------------|---------------------------------------------------------------------------------|
| `patient`            | `patient`              | CRUD with soft delete, link to external hospital ID              | [specs/1-05-2026-patient/](../specs/1-05-2026-patient/)                         |
| `hospital`           | `hospital`             | Gateway to external hospital DB (read + create SOAP)             | [specs/1-05-2026-hospital/](../specs/1-05-2026-hospital/)                       |
| `user`               | `user`                 | System users (doctors, admin), auth, roles                       | [specs/1-05-2026-user/](../specs/1-05-2026-user/)                               |
| `article`            | `article`              | Scientific articles + tags + PubMed sync                         | [specs/1-05-2026-article/](../specs/1-05-2026-article/)                         |
| `medication`         | `medicamento` *        | Drug catalog with status (active/obsolete/withdrawn)             | [specs/1-05-2026-medication/](../specs/1-05-2026-medication/)                   |
| `patient-context`    | `pacientecontexto` *   | Clinical context aggregator per patient                          | [specs/1-05-2026-patient-context/](../specs/1-05-2026-patient-context/)         |
| `specialty`          | `specialty`            | Catalog of medical specialties with COO admin CRUD               | [specs/12-05-2026-specialty/](../specs/12-05-2026-specialty/)                   |
| `medical-vocabulary` | `vocabulary`           | Per-specialty controlled vocabulary loaded from JSON at boot     | [specs/12-05-2026-medical-vocabulary/](../specs/12-05-2026-medical-vocabulary/) |

`*` Java package still in Spanish. Rename to English is deferred technical debt — see [conventions/naming.md](../conventions/naming.md#legacy-spanish-packages).

## Planned features

- `alert` — generates pharmacovigilance alerts by joining article tags ⨯ patient clinical context ⨯ medication status.

## Cross-cutting concerns

- **Two MySQL datasources** — `<default>` for MedSync data, `hospital` for the external hospital DB. See [ADR 0003](decisions/0003-two-datasources-medsync-hospital.md).
- **Flyway** manages schema for `<default>` only. Hospital schema is owned externally — Hibernate runs in `validate` mode against it.
- **JWT-based auth** — see [specs/1-05-2026-user/design.md](../specs/1-05-2026-user/design.md).
- **OpenAPI** — every endpoint annotated with `@Tag`, `@Operation`, `@APIResponse`. Swagger UI lives at `/q/swagger-ui`.

## Further reading

- [Stack details](stack.md)
- [ADR index](decisions/) — every non-obvious architectural choice
- [Coding conventions](../conventions/)
