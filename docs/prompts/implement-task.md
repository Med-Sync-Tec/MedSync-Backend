# Prompt template: Implement one task from tasks.md

Use this prompt when handing a single checklist item to an implementation agent (or to yourself in a focused session).

---

You are implementing one task from a MedSync Backend feature spec.

**Feature**: `<<feature-name>>`
**Task to execute**: `<<exact line from docs/specs/<feature>/tasks.md>>`

**Authoritative references** (read before changing any file):

1. `docs/specs/<<feature-name>>/requirements.md` — what the feature must do.
2. `docs/specs/<<feature-name>>/design.md` — how we agreed to build it.
3. `docs/conventions/*.md` — coding rules. The relevant ones for this task: <<naming | persistence | validation | testing | exceptions | migrations>>.
4. `CLAUDE.md` — project-level operator instructions.

**Workflow**:

1. **RED** — write the test first. Use the test layer matching what you're implementing (see `docs/conventions/testing.md`). Run it mentally; confirm it would fail.
2. **GREEN** — write the minimum code to make the test pass. Stay within the design. Do not refactor neighboring code.
3. **REFACTOR** — only if the new code introduces obvious duplication or unclear names.
4. **Report** — show me:
   - Files created / modified (with paths).
   - The test you wrote.
   - The implementation.
   - One line on anything that surprised you (deviations from the design).

**Do not**:

- Skip the test.
- Run `./mvnw` — I run dev/test commands myself. Hand me the commands to run if I need them.
- Edit unrelated files. If you find a bug while implementing, note it in the report — do not fix it inline.
- Add features beyond the task description. The design already drew the line.

**Definition of done**:

- One commit-sized change.
- The test would pass.
- File diffs are minimal — no churn in unrelated code.
- The corresponding line in `tasks.md` is marked `- [x]` (do this in the same commit).
