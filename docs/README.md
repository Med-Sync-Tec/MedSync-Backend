# MedSync Backend — Documentation

This folder contains all engineering documentation for MedSync Backend. We follow a **spec-driven development** workflow: every feature starts as a written specification before any code is written.

## Layout

```
docs/
├── README.md                  ← you are here
├── architecture/              high-level system design (stable, rarely changes)
│   ├── overview.md            layers, dependency rule, package map
│   ├── stack.md               Quarkus, MySQL, Flyway, etc.
│   └── decisions/             Architecture Decision Records (ADRs)
├── conventions/               coding rules (one file per concern)
│   ├── naming.md
│   ├── persistence.md
│   ├── validation.md
│   ├── testing.md
│   ├── exceptions.md
│   └── migrations.md
├── glossary.md                domain terms (EN ↔ ES)
├── prompts/                   reusable prompt templates for AI agents
│   ├── new-feature.md
│   ├── implement-task.md
│   └── review-spec.md
└── specs/                     one folder per feature
    ├── README.md              spec lifecycle + templates
    └── <feature>/
        └── <DD-MM-YYYY>/      dated snapshot of the spec (one folder per revision)
            ├── requirements.md    user stories + acceptance criteria (EARS format)
            ├── design.md          domain model, endpoints, sequence diagrams
            ├── tasks.md           ordered, actionable checklist
            └── summary.md         what shipped (filled after implementation)
```

## Spec-driven workflow

1. **`requirements.md`** — what the feature must do (no implementation detail). Written first, reviewed before design.
2. **`design.md`** — how we'll build it: domain model, endpoints, persistence, sequence flows. Reviewed before any code.
3. **`tasks.md`** — ordered checklist an agent (or human) can execute. Granular enough that each item maps to one commit.
4. **(Implementation happens here, following TDD.)**
5. **`summary.md`** — what was actually shipped, deviations from design, deferred items, technical debt.

The reason `summary.md` is post-implementation: design documents what we *intend*; the summary documents what *exists*. They diverge — that's expected. Future readers need both.

## When in doubt

- **Adding a new feature?** → Start at [`specs/README.md`](specs/README.md) and copy the template.
- **Code style question?** → [`conventions/`](conventions/).
- **Why is X built this way?** → [`architecture/decisions/`](architecture/decisions/).
- **What does "expediente" mean?** → [`glossary.md`](glossary.md).
- **Need to invoke an agent for spec work?** → [`prompts/`](prompts/).

## Language policy

All technical documentation is written in **English**. The exception is `glossary.md`, which is bilingual because the clinical domain (and the external hospital database) uses Spanish terms (`paciente`, `expediente`, `consulta`).

The project-level [`CLAUDE.md`](../CLAUDE.md) file remains in Spanish — it is a personal guide for the human operator.
