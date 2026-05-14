# Feature: Patient Context — Design

> Java package: `pacientecontexto`. Spec name: `patient-context`. See [conventions/naming.md](../../conventions/naming.md#legacy-spanish-packages).

## Domain model

### `PacienteContexto` (immutable, `domain/pacientecontexto/model/`)

| Field        | Type            | Required | Notes                              |
|--------------|-----------------|----------|------------------------------------|
| id           | `UUID`          | yes      | server-generated                   |
| pacienteId   | `UUID`          | yes      | FK → `Patient.id`                  |
| tipo         | `TipoClinico`   | yes      | enum                               |
| valor        | `String`        | yes      | 1..500 chars                       |
| createdAt    | `LocalDateTime` | auto     |                                    |

Invariants: all fields not null; `valor` not blank and ≤500 chars; `tipo` non-null enum value.

No behaviors beyond construction — this is a passive value-style aggregate (deletion is a repository concern).

### `TipoClinico` (shared enum, `domain/shared/model/`)

```java
public enum TipoClinico {
    ENFERMEDAD,
    SINTOMA,
    TRATAMIENTO,
    MEDICAMENTO
}
```

Shared with the `article` feature for tag matching. **Never add or rename a value here** without coordinating with `article` migrations.

## Output port

```java
public interface PacienteContextoRepository {
    PacienteContexto save(PacienteContexto context);
    Optional<PacienteContexto> findByUuid(UUID id);
    List<PacienteContexto> findByPacienteId(UUID pacienteId);   // ordered by createdAt DESC
    void removeById(UUID id);
}
```

## Use cases

| Use case                                       | Service                                       |
|------------------------------------------------|-----------------------------------------------|
| `AddPacienteContextoUseCase`                   | `AddPacienteContextoService`                  |
| `ListPacienteContextosByPacienteUseCase`       | `ListPacienteContextosByPacienteService`      |
| `DeletePacienteContextoUseCase`                | `DeletePacienteContextoService`               |

Each service injects `PatientRepository` (to verify the patient exists first).

`DeletePacienteContextoService` validates **ownership**: the context's `pacienteId` must match the path's `patientId`. If not, it throws `PacienteContextoNotFoundException` (404, not 403) — see decisions below.

## REST endpoints

| Method | Path                                                | Request DTO                     | Response                          | Status codes |
|--------|-----------------------------------------------------|---------------------------------|-----------------------------------|--------------|
| POST   | `/api/patients/{patientId}/contextos`                | `AddPacienteContextoRequest`    | `PacienteContextoResponse`        | 201 / 400 / 401 / 404 |
| GET    | `/api/patients/{patientId}/contextos`                | —                               | `List<PacienteContextoResponse>`  | 200 / 401 / 404 |
| DELETE | `/api/patients/{patientId}/contextos/{contextoId}`   | —                               | —                                 | 204 / 401 / 404 |

Swagger tag: `Patient Context`. All endpoints require `AuthenticatedUserContext`.

### DTOs (`interfaces/rest/pacientecontexto/`)
- `AddPacienteContextoRequest` — `tipo` (`@NotBlank @Size(max=30)`), `valor` (`@NotBlank @Size(max=500)`).
- `PacienteContextoResponse` — record `(id, pacienteId, tipo, valor, createdAt)` (`tipo` rendered as lowercase string, e.g. `"enfermedad"`).

## Persistence

### `PacienteContextoEntity` (table `paciente_contexto`)

| Column        | Type           | Constraints                                          |
|---------------|----------------|------------------------------------------------------|
| id            | `BINARY(16)`   | PK                                                   |
| paciente_id   | `BINARY(16)`   | NOT NULL, FK → `patients(id)`                        |
| tipo          | `VARCHAR(30)`  | NOT NULL (enum stored as String)                     |
| valor         | `VARCHAR(500)` | NOT NULL                                             |
| created_at    | `TIMESTAMP`    | NOT NULL DEFAULT CURRENT_TIMESTAMP (no `updated_at`) |

Indexes:
- `idx_paciente_contexto_paciente (paciente_id)` — list queries.
- `idx_paciente_contexto_lookup (paciente_id, tipo, valor)` — matching join + dedup checks.

### Migration: `V5__create_paciente_contexto_table.sql`

The FK on `paciente_id → patients(id)` is added immediately (`patients` already exists).

No relation entity on `PacienteContextoEntity` for `Patient` — the `paciente_id` is a scalar `BINARY(16)`. The repository implementation joins manually when needed.

## Validation

| Level     | Check                                                  | Location                         |
|-----------|--------------------------------------------------------|----------------------------------|
| Format    | `@NotBlank @Size(max=30)` on `tipo`                     | `AddPacienteContextoRequest`     |
| Format    | `@NotBlank @Size(max=500)` on `valor`                   | `AddPacienteContextoRequest`     |
| Business  | `tipo` resolves to a valid `TipoClinico` enum value     | service (catches `IllegalArgumentException` and converts) |
| Business  | `valor` non-blank, ≤500 chars                           | `PacienteContexto` constructor   |
| App-level | Patient exists                                          | service                          |
| App-level | Context belongs to the path's patient (delete)          | `DeletePacienteContextoService`  |

## Exceptions

| Domain exception                       | HTTP |
|----------------------------------------|------|
| `PacienteContextoNotFoundException`    | 404  |
| `InvalidPacienteContextoDataException` | 400  |
| `PatientNotFoundException` (reused)    | 404  |

## Key technical decisions

### 1. Separate feature from `patient`

`PacienteContexto` is a **tag-like attribute**, not a core patient field. Keeping it as a separate feature means:
- We can add many context entries without bloating the `Patient` aggregate or its load path.
- The matching engine queries this table directly without going through `Patient`.
- The `Patient` aggregate has zero direct dependency on context — it never loads `List<PacienteContexto>`.

### 2. Unidirectional relation

`PacienteContexto` knows its `pacienteId`. `Patient` does not know its contexts. Reasons:
- Loading a patient should not load N context rows.
- Context counts can grow without bound; aggregates with unbounded collections are an anti-pattern.

### 3. `TipoClinico` lives in `domain/shared/`

Shared with `article`'s `ArticleTag`. The matching algorithm joins on `(tipo, valor)` strings. If the enums diverged, matching would silently break. By keeping a single enum we guarantee they stay aligned.

### 4. 404 on ownership mismatch (not 403)

If a context exists but belongs to another patient, we return `404`, **not** `403`. Justification: returning `403` leaks the existence of the context (and therefore of another patient's id). `404` is consistent with the principle "don't reveal what the caller can't access". This is a security-by-design choice — flagged inline in the service so future maintainers don't "fix" it back to `403`.

### 5. Hard delete

Same reasoning as `medication`: context entries don't need historic preservation. A wrong entry should disappear.

### 6. No `updated_at` column

Context entries are not edited. They are added and deleted. `created_at` is sufficient.

### 7. `tipo` stored as `VARCHAR`, not `ENUM`

We use Hibernate's `@Enumerated(STRING)`. Avoids the MySQL `ENUM` type (rigid, schema migration headaches when adding values) and avoids `@Enumerated(ORDINAL)` (silent corruption if enum order changes).

### 8. Indexed `(paciente_id, tipo, valor)`

This index serves two purposes:
- Speeds `findByPacienteId` listings.
- Speeds the matching join used by `GetMatchingArticlesByPatient` in the `article` feature.

A simpler `(paciente_id)` index alone would force a full scan within a patient's contexts during matching. The compound index is worth the small write overhead.

## Sequence: Match articles by patient context (cross-feature reference)

`GetMatchingArticlesByPatientService` (in the `article` feature) does:

```sql
SELECT DISTINCT a.*
FROM articulos_cientificos a
JOIN articulo_tags t   ON t.articulo_id = a.id
JOIN paciente_contexto c ON c.tipo = t.tipo AND c.valor = t.valor
WHERE c.paciente_id = :patientId
ORDER BY a.updated_at DESC
LIMIT :limit
```

The `(paciente_id, tipo, valor)` and `articulo_tags (tipo, valor)` indexes drive this join.
