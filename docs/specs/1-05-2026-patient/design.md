# Feature: Patient — Design

## Domain model

`Patient` — immutable POJO, `domain/patient/model/Patient.java`.

| Field                   | Type            | Required | Notes                                             |
|-------------------------|-----------------|----------|---------------------------------------------------|
| id                      | `UUID`          | yes      | server-generated                                  |
| expedienteExternoId     | `String`        | yes      | hospital-side patient identifier (bridges to `hospital` feature) |
| nombre                  | `String`        | yes      |                                                   |
| fechaNacimiento         | `LocalDate`     | yes      | past, age ≤ 150                                   |
| genero                  | `String`        | no       |                                                   |
| medicoId                | `UUID`          | yes      | logical reference to a `User` with `DOCTOR` role  |
| activo                  | `boolean`       | yes      | soft-delete flag                                  |
| createdAt               | `LocalDateTime` | auto     | Hibernate                                         |
| updatedAt               | `LocalDateTime` | auto     | Hibernate                                         |

### Invariants (validated in constructor)
- `id`, `medicoId` not null.
- `nombre`, `expedienteExternoId` not null nor blank.
- `fechaNacimiento` not null and not in the future.
- Derived age ≤ 150.

### Behavior
- `Patient.softDelete()` returns a new instance with `activo = false`. (Immutable update.)

## Output port

`PatientRepository` (`domain/patient/repository/`):

```java
public interface PatientRepository {
    Patient save(Patient patient);
    Optional<Patient> findByUuid(UUID id);
    List<Patient> findAllActive();
    boolean existsByExpedienteExternoId(String value);   // includes soft-deleted
}
```

The method is named `findByUuid` (not `findById`) because `PanacheRepository<PatientEntity>` already defines `findById(UUID) → PatientEntity` with an incompatible return type. See decision below.

## Use cases

| Use case interface          | Service                       |
|-----------------------------|-------------------------------|
| `CreatePatientUseCase`      | `CreatePatientService`        |
| `GetPatientByIdUseCase`     | `GetPatientByIdService`       |
| `ListActivePatientsUseCase` | `ListActivePatientsService`   |
| `DeletePatientUseCase`      | `DeletePatientService`        |

## REST endpoints

| Method | Path                  | Request DTO              | Response DTO       | Status codes |
|--------|-----------------------|--------------------------|--------------------|--------------|
| POST   | `/api/patients`       | `CreatePatientRequest`   | `PatientResponse`  | 201 / 400 / 409 |
| GET    | `/api/patients`       | —                        | `List<PatientResponse>` | 200       |
| GET    | `/api/patients/{id}`  | —                        | `PatientResponse`  | 200 / 404    |
| DELETE | `/api/patients/{id}`  | —                        | —                  | 204 / 404    |

Swagger tag: `Patient`.

### DTOs (`interfaces/rest/patient/`)
- `CreatePatientRequest` — `public` fields with Jakarta Validation (`@NotBlank`, `@NotNull`, `@Past`, `@Size`).
- `PatientResponse` — `record` with all domain fields.

## Persistence

### Entity: `PatientEntity` (`infrastructure/persistence/patient/`)

| Column                  | Type           | Constraints                                |
|-------------------------|----------------|--------------------------------------------|
| id                      | `BINARY(16)`   | PK                                         |
| expediente_externo_id   | `VARCHAR(100)` | NOT NULL, UNIQUE                           |
| nombre                  | `VARCHAR(200)` | NOT NULL                                   |
| fecha_nacimiento        | `DATE`         | NOT NULL                                   |
| genero                  | `VARCHAR(20)`  |                                            |
| medico_id               | `BINARY(16)`   | NOT NULL (no FK constraint in V1)          |
| activo                  | `BOOLEAN`      | NOT NULL DEFAULT TRUE                      |
| created_at              | `TIMESTAMP`    | NOT NULL DEFAULT CURRENT_TIMESTAMP         |
| updated_at              | `TIMESTAMP`    | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |

Indexes: `idx_patients_medico`, `idx_patients_activo`.

Annotated with `@SQLRestriction("activo = true")` so soft-deleted rows are hidden by default.

### Migration: `V1__create_patients_table.sql`

```sql
CREATE TABLE patients (
    id BINARY(16) NOT NULL,
    expediente_externo_id VARCHAR(100) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    genero VARCHAR(20) NULL,
    medico_id BINARY(16) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_patients PRIMARY KEY (id),
    CONSTRAINT uq_patients_expediente UNIQUE (expediente_externo_id)
);

CREATE INDEX idx_patients_medico ON patients (medico_id);
CREATE INDEX idx_patients_activo ON patients (activo);
```

The FK on `medico_id` is added later by `V4__add_patient_medico_fk.sql` once the `user` feature creates the `usuarios` table.

### `@NamedEntityGraph`

None for this feature — `Patient` has no JPA relations.

## Validation

| Level     | Check                                                | Location                                |
|-----------|------------------------------------------------------|-----------------------------------------|
| Format    | `@NotBlank` on `nombre`, `expedienteExternoId`       | `CreatePatientRequest`                  |
| Format    | `@NotNull @Past` on `fechaNacimiento`                | `CreatePatientRequest`                  |
| Format    | `@NotNull` on `medicoId`                             | `CreatePatientRequest`                  |
| Format    | `@Size(max = N)` on text fields                      | `CreatePatientRequest`                  |
| Business  | `fechaNacimiento` not in the future                  | `Patient` constructor                   |
| Business  | Derived age ≤ 150                                    | `Patient` constructor                   |
| Business  | `nombre`, `expedienteExternoId` not blank            | `Patient` constructor                   |
| App-level | `expedienteExternoId` globally unique                | `CreatePatientService` + DB `UNIQUE`    |

## Exceptions

| Domain exception                | HTTP |
|---------------------------------|------|
| `PatientNotFoundException`      | 404  |
| `DuplicatePatientException`     | 409  |
| `InvalidPatientDataException`   | 400  |

## Key technical decisions

### 1. `medicoId` as a scalar `UUID`, not `@ManyToOne User`

When this feature was implemented, the `user` feature did not exist yet. To avoid blocking on user, `medicoId` is a scalar UUID with **no FK constraint** in V1. The FK is added by V4 once `usuarios` exists. The domain `Patient` model carries a UUID — not a `User` object — which honors the "no references to unimplemented features" rule.

### 2. `findByUuid` instead of `findById`

`PanacheRepository<PatientEntity>` already defines `findById(UUID) → PatientEntity`. If the domain interface used the same method name with `Optional<Patient>` as return type, Java rejects the class due to incompatible return types. We rename to `findByUuid` and accept losing the canonical name.

### 3. Soft delete via `@SQLRestriction("activo = true")`

Hibernate transparently appends `AND activo = true` to all JPQL queries against `PatientEntity`. `DeletePatientService` invokes `patient.softDelete()` and saves — physical `delete()` is never called.

### 4. Unique `expedienteExternoId` across active **and** soft-deleted rows

Naively, `@SQLRestriction` would hide soft-deleted rows from `existsBy*` checks. If a doctor then re-creates a patient with the same external id, the application check passes but the DB `UNIQUE` constraint rejects the insert with a raw 500. We solve this two ways:

- `PatientRepositoryImpl.existsByExpedienteExternoId` uses a **native SQL query** that bypasses `@SQLRestriction`.
- `GlobalExceptionHandler` maps Hibernate's `ConstraintViolationException` to `409` as a safety net for races.

### 5. `save` bypasses `@SQLRestriction` (uses `findByIdOptional`)

`save()` uses Panache's `findByIdOptional`, which goes through `session.find` and bypasses `@SQLRestriction`. This allows updating a row that is soft-deleted (necessary mid-flow during a soft-delete operation, and forward-compatible with eventual "restore" functionality). The public read methods (`findByUuid`, `findAllActive`) use HQL that respects the restriction.

### 6. Hibernate in `validate` mode

`quarkus.hibernate-orm.database.generation=validate`. The migration is the single source of truth — Hibernate refuses to start if `@Entity` and schema don't match. Prevents drift.

### 7. H2 in `MODE=MySQL` for dev / test

JDBC URL uses `MODE=MySQL` so `BINARY(16)`, `BOOLEAN`, `TIMESTAMP DEFAULT CURRENT_TIMESTAMP` behave the same in tests and in MySQL prod. One SQL file works on both engines.

## Sequence: Create patient

```
Client      PatientResource    CreatePatientService    PatientRepository       DB
  │  POST  │                   │                       │                       │
  │ ──────►│ @Valid CreatePatientRequest               │                       │
  │        │ ──── execute(req fields) ────►            │                       │
  │        │                   │ existsBy(...) (native query)                  │
  │        │                   │ ──────────────────►   │ SELECT COUNT(*)       │
  │        │                   │                       │ ────────────────────► │
  │        │                   │ new Patient(...)  ← invariants in ctor        │
  │        │                   │ save(patient)         │                       │
  │        │                   │ ──────────────────►   │ INSERT INTO patients  │
  │        │ ◄── Patient ──────│                       │                       │
  │ ◄── 201 + Location ────────│                       │                       │
```
