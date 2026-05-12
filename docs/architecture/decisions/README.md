# Architecture Decision Records (ADRs)

ADRs document architectural choices that were not obvious from reading the code. They capture **context** (why we faced the decision), **options considered**, and **consequences** (positive, negative, neutral).

## Index

| #    | Title                                                                          | Status   |
|------|--------------------------------------------------------------------------------|----------|
| 0001 | [Clean Architecture with Layer-by-Feature](0001-clean-architecture-layer-feature.md) | Accepted |
| 0002 | [Gateway vs Repository naming](0002-gateway-vs-repository.md)                  | Accepted |
| 0003 | [Two MySQL datasources (MedSync + Hospital)](0003-two-datasources-medsync-hospital.md) | Accepted |

## When to write an ADR

Write one when:

- The decision is hard to reverse later (DB engine, framework, language).
- A future reader would ask "why on earth did they do it this way?".
- Multiple options were seriously considered and you want to document why the alternatives were rejected.
- The decision creates a constraint others must respect.

Don't write one for routine choices (linter rules, single-method refactors, naming a class).

## Template

```markdown
# ADR NNNN: Title

- **Status**: Proposed | Accepted | Deprecated | Superseded by ADR XXXX
- **Date**: YYYY-MM-DD
- **Deciders**: who agreed

## Context
What problem are we solving? What constraints existed?

## Decision
What we decided to do.

## Consequences
### Positive
### Negative
### Neutral

## Related decisions
Links to other ADRs.
```

Numbering is sequential and immutable — once an ADR has a number, that number is permanent. If you supersede an ADR, mark the old one Deprecated and link to the new one.
