# Prompt template: Review a spec before implementation

Use this prompt when a `requirements.md` or `design.md` is "done" and you want a critical second pair of eyes before any code gets written.

---

You are reviewing the spec for `docs/specs/<<feature-name>>/`. Your job is to find problems **before** implementation starts, when fixes are still cheap.

**Authoritative references**:

- `docs/specs/<<feature-name>>/requirements.md`
- `docs/specs/<<feature-name>>/design.md`
- `docs/architecture/overview.md` (dependency rule)
- `docs/architecture/decisions/` (ADRs)
- `docs/conventions/*.md`
- Existing specs of related features in `docs/specs/`

**Review checklist** — answer each with a verdict (✅ / ⚠️ / ❌) and a one-sentence justification:

### Requirements

- [ ] Each acceptance criterion is in EARS format (When/While/If/Where … the system shall …).
- [ ] Each criterion is testable — you can imagine the test that proves it.
- [ ] No implementation detail leaked in (e.g. "the system shall use a JPA repository" — that's design, not requirements).
- [ ] Out-of-scope items are explicit.
- [ ] User stories tie each criterion to a value-bearing actor (doctor, admin, patient, system).

### Design

- [ ] Domain model has no JPA / Quarkus / Jakarta imports.
- [ ] Each use case has its own service class. No god services.
- [ ] Output ports correctly distinguish `*Repository` (own data) from `*Gateway` (external).
- [ ] Persistence section follows `conventions/persistence.md`: `BINARY(16)` ids, `EntityGraph` for relations, no `EAGER`, soft-delete via `@SQLRestriction` if applicable.
- [ ] REST section uses DTOs (not domain models) on both sides; mapper named `<Feature>RestMapper`.
- [ ] Validation lives at two levels (DTO + domain). The doc names both.
- [ ] Migration is named `V<N>__<snake_case>.sql` and is MySQL/H2-compatible.
- [ ] Exception handling lists every domain exception with its HTTP code and adds it to `GlobalExceptionHandler`.
- [ ] Non-trivial decisions reference an ADR or include a "Why" rationale inline.

### Cross-cutting

- [ ] Does the design conflict with any existing ADR? If yes, flag it.
- [ ] Are there integration points with other features that the related specs don't yet mention? (Both sides may need updating.)
- [ ] Are there security implications (auth, authorization, PII)? Did the spec address them?
- [ ] Is there obvious deferred work that should be noted as technical debt in `summary.md` when the feature ships?

**Output format**: a markdown report grouped by Requirements / Design / Cross-cutting, each item with its verdict and your one-sentence reasoning. End with a "Blockers" section listing items you'd want fixed before implementation begins, and a "Nits" section for non-blocking suggestions.

Do not modify the spec files. Your job is to flag, not to fix.
