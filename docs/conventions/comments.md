# Comment Conventions

Comments in MedSync code follow two rules:

1. **Javadoc** is for the *contract* — what a class or public method promises to its callers.
2. **Inline comments** are for the *why* — non-obvious invariants, design decisions, or trade-offs that the code itself cannot express.

If a comment paraphrases the code, delete it. If a comment explains something a future maintainer would otherwise need to reverse-engineer, keep it.

Language: **English**. Identifiers stay in their original language (the domain still uses Spanish words like `nombre`, `fechaNacimiento`, `activo` — that is a separate naming decision, see [naming.md](./naming.md)).

## What to comment

| Element                                       | Javadoc | Inline | Notes                                                                                          |
|-----------------------------------------------|---------|--------|------------------------------------------------------------------------------------------------|
| Domain aggregate / value object class         | Yes     | —      | Describe the concept it models and the invariants enforced in the constructor.                 |
| Domain repository / gateway interface         | Yes     | —      | One line per method explaining contract (return shape, what "not found" looks like).           |
| Use case interface                            | Yes     | —      | Describe the business action and what it returns / throws.                                     |
| Application service                           | Yes     | rare   | Javadoc on the class; inline only if orchestration order is non-obvious.                       |
| JPA entity                                    | Yes     | rare   | Javadoc on the class noting the table and any soft-delete / `@SQLRestriction` rule.            |
| Persistence mapper (`*PersistenceMapper`)     | brief   | —      | One-line Javadoc on the class; the methods are self-explanatory.                               |
| Repository implementation                     | Yes     | rare   | Note any custom query semantics that differ from Panache defaults.                             |
| REST resource                                 | Yes     | rare   | Javadoc on the class declaring the URL prefix and auth requirements; per-method Javadoc only if behavior is non-obvious (already covered by `@Operation`). |
| Request / response DTO                        | brief   | —      | One line on the record; fields document themselves through validation annotations.             |
| REST mapper (`*RestMapper`)                   | brief   | —      | One line on the class.                                                                         |
| Domain exception                              | brief   | —      | One line on the class noting which HTTP status it maps to.                                     |
| Public method on a domain model               | Yes     | —      | Describe what the operation returns or which invariant it enforces.                            |
| Private helper                                | no      | —      | Name it well instead. Add a comment only if the body is genuinely surprising.                  |
| Getter / setter                               | no      | —      | Self-evident from the field name.                                                              |
| `equals` / `hashCode` / `toString`            | no      | —      | Self-evident from the implementation.                                                          |

## What NOT to comment

Skip comments that:

- Restate the method name (`// Returns the id`).
- Explain Java syntax (`// final field, can't reassign`).
- Document an obvious mapping (`// Maps the Entity to Domain`).
- Refer to the current task, PR number, or author (those belong in commit messages and `git blame`).
- Mark "TODO" without a follow-up issue link. Either fix it now or open a ticket.

## Inline comment style

Reserve inline comments for the **why**. Useful triggers:

- A specific invariant the code enforces but the type system cannot express:
  ```java
  // Defensive copy: the caller may mutate the input list after construction.
  this.terms = List.copyOf(terms);
  ```
- A subtle ordering requirement:
  ```java
  // Validate first; we want a clean ConstraintViolationException
  // before any DB write side effects.
  validate(request);
  repository.save(domain);
  ```
- A non-obvious trade-off documented in a spec or ADR:
  ```java
  // Fail-fast at boot: a typo in a vocabulary file should never
  // silently degrade AI extraction in production. See design.md §3.
  throw new VocabularyParseException(filename, field, message);
  ```
- A workaround for a known framework limitation:
  ```java
  // Hibernate cannot eager-fetch this collection through the @SQLRestriction
  // filter; we hydrate it explicitly with an EntityGraph hint.
  ```

A comment that starts with **"because"**, **"so that"**, **"to avoid"**, or **"reason:"** is usually a good comment. A comment that starts with **"this"** is usually a bad one.

## Javadoc style

- **First sentence** is a one-line summary in imperative form (`Returns ...`, `Validates ...`, `Throws ...`). It appears in IDE tooltips.
- **Blank line**, then any additional context: invariants, side effects, threading notes.
- Use `@param` and `@throws` only when the parameter or exception needs more than its type name to understand.
- Do **not** add `@author`, `@since`, `@version` — they rot. `git blame` and `git log` are authoritative.

Example:

```java
/**
 * In-memory, per-specialty controlled vocabulary used to constrain AI tag extraction.
 *
 * The aggregate is immutable. The constructor normalizes the input map so that every
 * {@link TipoClinico} key is present (missing buckets become {@code List.of()}), and
 * defensively copies every list so callers cannot mutate the stored state.
 *
 * Lookups are case-insensitive and whitespace-tolerant; stored values preserve their
 * original capitalization, since the LLM and the debug endpoint emit the canonical form
 * verbatim.
 */
public final class Vocabulary { ... }
```

## Examples to follow

- [`Vocabulary.java`](../../src/main/java/itesm/medsync/domain/vocabulary/model/Vocabulary.java) — aggregate Javadoc + a single inline comment on the case-insensitive comparison.
- [`VocabularyLoader.java`](../../src/main/java/itesm/medsync/infrastructure/vocabulary/VocabularyLoader.java) — class-level Javadoc on the boot lifecycle + inline comments on fail-fast vs. soft-skip branches.
- [`SpecialtyRepositoryImpl.java`](../../src/main/java/itesm/medsync/infrastructure/persistence/specialty/SpecialtyRepositoryImpl.java) — short class Javadoc + one inline note on the cross-row uniqueness query.

## Where comments do not belong

- **Inside tests.** A test's `@DisplayName` is its comment. If a test needs additional explanation, the test itself is too complex — split it.
- **In migration SQL.** Use the migration filename (`V12__add_especialidad_id_and_drop_legacy.sql`) and the file's own SQL comments. Do not duplicate the explanation in Java.
- **In `application.properties`.** A section header (`# === SECTION ===`) is fine; per-line commentary is noise.
- **In generated files.** OpenAPI specs, build outputs, Flyway history table — never touched by hand.

## When in doubt

Ask: "If I deleted this comment, would a future maintainer be able to recover this information from the code, the type system, or `git blame`?" If yes, delete the comment. If no, keep it.
