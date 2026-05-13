# Feature: Medication — Design

> Java package: `medicamento`. Spec name: `medication`. See [conventions/naming.md](../../conventions/naming.md#legacy-spanish-packages).

## Domain models

### `Medicamento` (immutable, `domain/medicamento/model/`)

| Field        | Type            | Required | Notes                                  |
|--------------|-----------------|----------|----------------------------------------|
| id           | `UUID`          | yes      | server-generated                       |
| nombre       | `String`        | yes      | unique, ≤150 chars                     |
| estadoId     | `UUID`          | yes      | FK → `medicamento_estados.id`          |
| descripcion  | `String`        | no       | TEXT                                   |
| createdAt    | `LocalDateTime` | auto     |                                        |
| updatedAt    | `LocalDateTime` | auto     |                                        |

Invariants: `id`, `nombre`, `estadoId` not null; `nombre` not blank.
Behaviors:
- `Medicamento.create(nombre, descripcion, defaultEstadoId)` — generates UUID.
- `withEstado(newEstadoId)` — returns a new instance with updated status.
- `update(nombre, estadoId, descripcion)` — returns a new instance.

### `MedicamentoEstado` (immutable value object)

| Field        | Type            | Required |
|--------------|-----------------|----------|
| id           | `UUID`          | yes      |
| nombre       | `String`        | yes (unique, ≤50) |
| descripcion  | `String`        | no       |

### `MedicamentoEstadoNames` (constants)

```java
public final class MedicamentoEstadoNames {
    public static final String VIGENTE     = "VIGENTE";
    public static final String EN_REVISION = "EN_REVISION";
    public static final String OBSOLETO    = "OBSOLETO";
}
```

### `MedicamentoWithEstado` (record, transport)

```java
public record MedicamentoWithEstado(Medicamento medicamento, MedicamentoEstado estado) {}
```

### `MedicamentosPage` (record, pagination)

```java
public record MedicamentosPage(
    List<MedicamentoWithEstado> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
```

## Output ports

```java
public interface MedicamentoRepository {
    Medicamento save(Medicamento m);
    Optional<MedicamentoWithEstado> findByUuid(UUID id);
    MedicamentosPage list(int page, int size, String nombreFilter, String estadoFilter);
    boolean existsByNombre(String nombre);
    boolean existsByNombreExcludingId(String nombre, UUID excludeId);
    void deleteById(UUID id);
}

public interface MedicamentoEstadoRepository {
    Optional<MedicamentoEstado> findByNombre(String nombre);
    Optional<MedicamentoEstado> findByUuid(UUID id);
}
```

## Use cases

| Use case                              | Service                          |
|---------------------------------------|----------------------------------|
| `CreateMedicamentoUseCase`            | `CreateMedicamentoService`       |
| `ListMedicamentosUseCase`             | `ListMedicamentosService`        |
| `GetMedicamentoByIdUseCase`           | `GetMedicamentoByIdService`      |
| `UpdateMedicamentoUseCase`            | `UpdateMedicamentoService`       |
| `UpdateMedicamentoEstadoUseCase`      | `UpdateMedicamentoEstadoService` |
| `DeleteMedicamentoUseCase`            | `DeleteMedicamentoService`       |

## REST endpoints

| Method | Path                          | Request DTO                  | Response                       | Status codes |
|--------|-------------------------------|------------------------------|--------------------------------|--------------|
| GET    | `/api/medicamentos`           | `?page&size&nombre&estado`   | `MedicamentosPageResponse`     | 200 / 401    |
| GET    | `/api/medicamentos/{id}`      | —                            | `MedicamentoResponse`          | 200 / 401 / 404 |
| POST   | `/api/medicamentos`           | `CreateMedicamentoRequest`   | `MedicamentoResponse`          | 201 / 400 / 401 / 409 |
| PUT    | `/api/medicamentos/{id}`      | `UpdateMedicamentoRequest`   | `MedicamentoResponse`          | 200 / 400 / 401 / 404 / 409 |
| DELETE | `/api/medicamentos/{id}`      | —                            | —                              | 204 / 401 / 404 |
| PATCH  | `/api/medicamentos/{id}/estado` | `UpdateEstadoRequest`      | `MedicamentoResponse`          | 200 / 400 / 401 / 404 |

Swagger tag: `Medications`. All endpoints require `AuthenticatedUserContext` to be populated.

### DTOs
- `CreateMedicamentoRequest` — `nombre` (`@NotBlank @Size(max=150)`), `descripcion` (`@Size(max=1000)`).
- `UpdateMedicamentoRequest` — `nombre`, `estado` (status name), `descripcion`.
- `UpdateEstadoRequest` — `estado` (`@NotBlank @Size(max=50)`).
- `MedicamentoResponse` — record `(id, nombre, estado, descripcion, createdAt, updatedAt)` (note: `estado` is the status name, not UUID).
- `MedicamentosPageResponse` — record matching `MedicamentosPage`.

## Persistence

### `MedicamentoEstadoEntity` (table `medicamento_estados`)

| Column      | Type           | Constraints      |
|-------------|----------------|------------------|
| id          | `BINARY(16)`   | PK               |
| nombre      | `VARCHAR(50)`  | NOT NULL, UNIQUE |
| descripcion | `TEXT`         |                  |

### `MedicamentoEntity` (table `medicamentos`)

| Column      | Type           | Constraints                                  |
|-------------|----------------|----------------------------------------------|
| id          | `BINARY(16)`   | PK                                           |
| nombre      | `VARCHAR(150)` | NOT NULL, UNIQUE                             |
| estado_id   | `BINARY(16)`   | NOT NULL, FK → `medicamento_estados(id)`     |
| descripcion | `TEXT`         |                                              |
| created_at  | `TIMESTAMP`    | auto                                         |
| updated_at  | `TIMESTAMP`    | auto                                         |

Index: `idx_medicamentos_estado (estado_id)`.

Relation: `@ManyToOne(fetch = LAZY)` to `MedicamentoEstadoEntity` via `estado_id`.
`@NamedEntityGraph("Medicamento.withEstado")` for eager fetch.

### Migrations

- `V7__create_medicamentos_schema.sql` — both tables + indexes.
- `V8__seed_medicamentos.sql` — 3 statuses + 15 medications.

#### Seeded statuses

```sql
INSERT INTO medicamento_estados (id, nombre, descripcion) VALUES
    (UNHEX('...'), 'VIGENTE',     'Medicamento aprobado, disponible clínicamente'),
    (UNHEX('...'), 'EN_REVISION', 'Medicamento bajo reevaluación por nueva evidencia'),
    (UNHEX('...'), 'OBSOLETO',    'Medicamento retirado o discontinuado');
```

#### Seeded medications (15 total)

| Status        | Examples (representative)                                       |
|---------------|-----------------------------------------------------------------|
| `VIGENTE` (8) | Metformina, Amoxicilina, Atorvastatina, Losartán, Omeprazol, Ibuprofeno, Paracetamol, Levotiroxina |
| `EN_REVISION` (3) | Empagliflozina (SGLT-2), Rosuvastatina (DM2 interaction), Ciprofloxacino (resistance) |
| `OBSOLETO` (4) | Cloranfenicol, Metamizol, Rofecoxib (Vioxx), Terfenadina       |

## Validation

| Level     | Check                                                  | Location                          |
|-----------|--------------------------------------------------------|-----------------------------------|
| Format    | `@NotBlank @Size(max=150)` on `nombre`                  | `CreateMedicamentoRequest`        |
| Format    | `@Size(max=1000)` on `descripcion`                      | `CreateMedicamentoRequest`        |
| Format    | `@NotBlank @Size(max=50)` on `estado` (status name)     | `UpdateMedicamentoRequest`, `UpdateEstadoRequest` |
| Business  | `nombre`, `estadoId` not null/blank                     | `Medicamento` constructor         |
| App-level | `nombre` globally unique (on create + update)           | services + DB UNIQUE              |
| App-level | Status name resolves to an existing `MedicamentoEstado` | services                          |
| App-level | Authenticated caller required                            | `AuthenticatedUserContext`        |

## Exceptions

| Domain exception                  | HTTP |
|-----------------------------------|------|
| `MedicamentoNotFoundException`    | 404  |
| `DuplicateMedicamentoException`   | 409  |
| `InvalidMedicamentoDataException` | 400  |
| `EstadoNotFoundException`         | 404  |

## Key technical decisions

### 1. Status as a row, not an enum

`MedicamentoEstado` is a regular table. Three rows seeded today. We chose this over a Java enum so:
- Status descriptions can be edited operationally without a code deploy.
- New statuses (e.g. `RECALL`) can be added by a DBA-style migration.
- Reporting tools can join against `medicamento_estados.descripcion` directly.

Cost: one extra join per query (mitigated by EntityGraph). Acceptable.

### 2. Hard delete (no soft delete)

Pharmacovigilance regulators do not require historic catalog state for the kinds of decisions this catalog drives. If that changes, we'll add a soft-delete flag in a later migration. The `Medicamento` domain has no `delete()` method — deletion goes straight through the repository.

### 3. Default status `VIGENTE` on create

Hard-coded constant in `CreateMedicamentoService`. The endpoint does not accept an `estado` field on create. Use the dedicated `PATCH /{id}/estado` to transition.

### 4. Status name on the wire, UUID in the database

The REST API exposes status as its **name** (`"VIGENTE"`), not its UUID. The service translates name ↔ UUID via `MedicamentoEstadoRepository.findByNombre`. UUIDs are an internal concern; clients don't see them.

### 5. Manual pagination via JPQL

Panache's built-in `Page` helper is not used. We assemble `MedicamentosPage` by issuing two queries: one for the page content (with filters + `LIMIT` / `OFFSET`), one for `COUNT(*)`. Trade-off: more code, but filters and ordering are explicit and the response shape is symmetric with the design.

### 6. Authenticated user required on every endpoint

`MedicamentoResource` reads `AuthenticatedUserContext.currentUser()` and throws `401` if null. No role check today — any authenticated user can read or write. Role gating is deferred to when product calls out an "approver" role.

### 7. Auditability deferred

We do not record who changed status, when, or why. Acknowledged debt — flagged in `1-05-2026/summary.md`.
