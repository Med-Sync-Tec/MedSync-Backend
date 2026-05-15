# ADR 0001: Clean Architecture with Layer-by-Feature Organization

- **Status**: Accepted
- **Date**: 2026-04-16
- **Deciders**: Backend team

## Context

We need a project structure that:

1. Keeps business rules independent from frameworks (so we can swap Quarkus, JPA, or REST without rewriting the domain).
2. Makes it easy to find all code related to a single feature.
3. Scales as features multiply — adding the 12th feature should not be harder than adding the 2nd.

Two common patterns:

- **Layer-by-type**: top-level folders are `controllers/`, `services/`, `repositories/`. Easy to learn, but understanding one feature requires opening four directories. Coupling between features is invisible from the file tree.
- **Layer-by-feature**: top-level folders are `patient/`, `article/`, etc. Each feature is self-contained. Coupling between features is explicit (cross-feature imports stand out).

## Decision

We use **Clean Architecture** (Uncle Bob) with **four layers** — `domain`, `application`, `infrastructure`, `interfaces` — and within each layer, **organize by feature**.

```
src/main/java/itesm/medsync/
├── domain/<feature>/
├── application/<feature>/
├── infrastructure/persistence/<feature>/
└── interfaces/rest/<feature>/
```

Strict dependency rule, in order from innermost to outermost:

```
domain  ← application  ← infrastructure
                       ← interfaces
```

`domain` has zero external dependencies. `application` depends only on `domain`. `infrastructure` and `interfaces` depend on `domain` plus their respective frameworks (JPA, JAX-RS).

## Consequences

### Positive

- A new engineer can find every piece of `patient` by searching for the word `patient` in the file tree — model, REST resource, repository, exceptions, tests.
- Domain code is fully unit-testable without Quarkus (`./mvnw test` boots in milliseconds for domain tests, not seconds).
- Swapping JPA for another persistence library is a localized change in `infrastructure/persistence/<feature>/`.
- Cross-feature dependencies are visible. `application/alert/` importing `domain/article/` is a deliberate design choice, not an accident.

### Negative

- Adding a feature requires four folders and ~8 files minimum. This is a fixed cost — automation (templates) helps but does not eliminate it.
- Engineers used to layer-by-type need orientation.
- Some teams find the four-layer split overkill for small features. We accept the consistency cost.

### Neutral

- Naming convention enforced: `<Feature>Repository`, `<Feature>Entity`, `<Action><Feature>UseCase`, `<Action><Feature>Service`, `<Feature>Resource`. See [conventions/naming.md](../../conventions/naming.md).

## Related decisions

- [ADR 0002 — Gateway vs Repository naming](0002-gateway-vs-repository.md)
- [ADR 0003 — Two datasources (MedSync + Hospital)](0003-two-datasources-medsync-hospital.md)
