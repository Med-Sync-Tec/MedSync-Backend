# Prompt template: New feature spec

Copy-paste this prompt into a planning conversation when starting a brand-new feature. Replace `<<...>>` placeholders.

---

You are helping me write the specification for a new feature in the MedSync Backend project.

**Project context** (do not look these up — they are stable):

- Quarkus 3.34 / Java 21, Clean Architecture with Layer-by-Feature organization.
- Conventions live in `docs/conventions/` — read `naming.md`, `persistence.md`, `validation.md`, `testing.md`, `exceptions.md`, `migrations.md` before proposing anything.
- ADRs live in `docs/architecture/decisions/`.
- Glossary at `docs/glossary.md`.

**Feature name**: `<<feature-name-in-english>>` (Java package will be `<<feature-name-camelcase>>` if multi-word).

**Goal**: <<one-paragraph description of what the feature must do and why>>.

**Constraints / known requirements**:
- <<bullet 1>>
- <<bullet 2>>

**Out of scope (explicit)**:
- <<thing the feature won't do>>

**Related features it touches**:
- <<existing feature 1 — how it depends on it>>
- <<existing feature 2 — how it depends on it>>

**Your job**:

1. Read `docs/specs/README.md` for the spec lifecycle.
2. Create `docs/specs/<<feature-name>>/requirements.md` following the EARS format (When/While/If/Where … the system shall …).
3. Stop. Wait for me to approve `requirements.md` before writing `design.md` or `tasks.md`.

**Don't**:

- Write any Java code yet.
- Make decisions that contradict the conventions docs — flag the conflict and ask first.
- Invent integration points that aren't documented in `architecture/overview.md` or existing feature specs.

When you write `requirements.md`, include sections: Overview, User stories, Acceptance criteria (EARS), Non-functional requirements, Open questions.
