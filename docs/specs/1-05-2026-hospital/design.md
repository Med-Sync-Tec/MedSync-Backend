# Feature: Hospital — Design

## Two-datasource topology

```
┌──────────────────┐        ┌──────────────────┐
│  medsync-db      │        │  hospital-db     │
│  :3307 (prod)    │        │  :3308 (prod)    │
│  patients        │        │  expedientes_    │
│  users           │        │    clinicos      │
│  alerts          │        │  consultas       │
└────────┬─────────┘        └────────┬─────────┘
         │ PU <default>              │ PU "hospital"
         ▼                           ▼
    ┌────────────────────────────────────────┐
    │       Quarkus / Hibernate              │
    └────────────────────────────────────────┘
```

Two persistence units in Quarkus, two disjoint `@Entity` packages:
- `itesm.medsync.infrastructure.persistence.*` → PU `<default>`
- `itesm.medsync.infrastructure.hospital.*` → PU `hospital`

See [ADR 0003](../../architecture/decisions/0003-two-datasources-medsync-hospital.md).

## Domain models

### `ExpedienteClinico` (immutable, `domain/hospital/model/`)

| Field                   | Type            | Required | Notes                                     |
|-------------------------|-----------------|----------|-------------------------------------------|
| id                      | `String`        | yes      | varchar, **not** a UUID                   |
| pacienteExternoId       | `String`        | yes      | links to `Patient.expedienteExternoId`    |
| doctorResponsableId     | `String`        | no       | hospital-side doctor id                   |
| createdAt               | `LocalDateTime` | auto     |                                           |
| updatedAt               | `LocalDateTime` | auto     |                                           |

Invariants: `id` and `pacienteExternoId` not blank.
Factory: `ExpedienteClinico.create(pacienteExternoId, doctorResponsableId)` generates a UUID string for `id`.

### `Consulta` (immutable)

SOAP + prescription + diagnosis:

| Field           | Type            | Required | Notes      |
|-----------------|-----------------|----------|------------|
| id              | `String`        | yes      | varchar    |
| expedienteId    | `String`        | yes      | logical FK |
| fecha           | `LocalDateTime` | yes      | client-supplied (allows retroactive entries) |
| motivoConsulta  | `String`        | no       |            |
| subjetivo       | `String`        | no       | S of SOAP  |
| objetivo        | `String`        | no       | O of SOAP  |
| evaluacion      | `String`        | no       | A of SOAP  |
| plan            | `String`        | no       | P of SOAP  |
| prescripcion    | `String`        | no       |            |
| diagnostico     | `String`        | no       |            |
| createdAt       | `LocalDateTime` | auto     |            |
| updatedAt       | `LocalDateTime` | auto     |            |

Invariants: `id`, `expedienteId` not blank; `fecha` not null.
Factory: `Consulta.create(expedienteId, fecha, ...)` generates a UUID string for `id`.

## Output port

`HospitalGateway` (`domain/hospital/repository/HospitalGateway.java`):

```java
public interface HospitalGateway {
    Optional<ExpedienteClinico> findExpedienteByPacienteExternoId(String pacienteExternoId);
    List<Consulta> findConsultasByPacienteExternoId(String pacienteExternoId);
    Optional<Consulta> findConsultaById(String consultaId);
    ExpedienteClinico saveExpediente(ExpedienteClinico expediente);
    Consulta saveConsulta(Consulta consulta);
}
```

**Why `Gateway` and not `Repository`**: the hospital is an external system. See [ADR 0002](../../architecture/decisions/0002-gateway-vs-repository.md).

## Use cases

| Use case interface                  | Service                              |
|-------------------------------------|--------------------------------------|
| `GetExpedienteByPatientUseCase`     | `GetExpedienteByPatientService`      |
| `GetConsultasByPatientUseCase`      | `GetConsultasByPatientService`       |
| `GetConsultaByIdUseCase`            | `GetConsultaByIdService`             |
| `CreateConsultaUseCase`             | `CreateConsultaService`              |

Each service injects `GetPatientByIdUseCase` (from the `patient` feature) and `HospitalGateway`.

## REST endpoints

| Method | Path                                       | Request DTO            | Response DTO                | Status |
|--------|--------------------------------------------|------------------------|-----------------------------|--------|
| GET    | `/api/patients/{id}/expediente`            | —                      | `ExpedienteClinicoResponse` | 200 / 404 |
| GET    | `/api/patients/{id}/consultas`             | —                      | `List<ConsultaResponse>`    | 200 / 404 |
| GET    | `/api/consultas/{consultaId}`              | —                      | `ConsultaResponse`          | 200 / 404 |
| POST   | `/api/patients/{id}/consultas`             | `CreateConsultaRequest` | `ConsultaResponse`         | 201 / 400 / 404 |

Swagger tag: `Hospital`. Two resource classes for path-resolution reasons (see decisions below).

### DTOs (`interfaces/rest/hospital/`)
- `CreateConsultaRequest` — Jakarta-validated. No `consultaId` (server generates it).
- `ExpedienteClinicoResponse` — record, 5 fields.
- `ConsultaResponse` — record, 12 fields.

## Persistence

### `ExpedienteClinicoHospitalEntity` (`infrastructure/hospital/`)

| Column                  | Type          | Constraints |
|-------------------------|---------------|-------------|
| id                      | `VARCHAR(50)` | PK          |
| paciente_externo_id     | `VARCHAR(100)`| UNIQUE      |
| doctor_responsable_id   | `VARCHAR(50)` | nullable    |
| created_at              | `TIMESTAMP`   | auto        |
| updated_at              | `TIMESTAMP`   | auto        |

### `ConsultaHospitalEntity`

| Column           | Type          | Constraints                                  |
|------------------|---------------|----------------------------------------------|
| id               | `VARCHAR(50)` | PK                                           |
| expediente_id    | `VARCHAR(50)` | NOT NULL, FK → `expedientes_clinicos(id)`   |
| fecha            | `DATETIME`    | NOT NULL                                     |
| motivo_consulta  | `TEXT`        | nullable                                     |
| subjetivo        | `TEXT`        | nullable                                     |
| objetivo         | `TEXT`        | nullable                                     |
| evaluacion       | `TEXT`        | nullable                                     |
| plan             | `TEXT`        | nullable                                     |
| prescripcion     | `TEXT`        | nullable                                     |
| diagnostico      | `TEXT`        | nullable                                     |
| created_at       | `TIMESTAMP`   | auto                                         |
| updated_at       | `TIMESTAMP`   | auto                                         |

No `@ManyToOne` relations — `expedienteId` is a scalar `String`. Queries use JPQL with explicit `IN (subquery)` patterns instead of object navigation. No `@NamedEntityGraph` needed (no relations).

### Schema source

The hospital schema lives in `docker/hospital-initdb/01_schema.sql` (loaded by Docker on container start). MedSync does **not** ship a Flyway migration for hospital tables. Hibernate runs in `validate` mode (`%mysql-local`, `%prod`) or `drop-and-create` (`%dev`, `%test`).

### `HospitalGatewayImpl`

Does **not** extend `PanacheRepositoryBase` (it handles two entities). Injects:

```java
@PersistenceContext(unitName = "hospital")
EntityManager entityManager;
```

Custom JPQL example:

```java
SELECT c FROM ConsultaHospitalEntity c
WHERE c.expedienteId IN (
    SELECT e.id FROM ExpedienteClinicoHospitalEntity e
    WHERE e.pacienteExternoId = :pacExt
)
ORDER BY c.fecha DESC
```

## Validation

| Level     | Check                                                | Location                          |
|-----------|------------------------------------------------------|-----------------------------------|
| Format    | `@NotNull fecha`, `@Size(max=...)` on text fields    | `CreateConsultaRequest`           |
| Business  | `id`, `expedienteId`, `pacienteExternoId` not blank  | `Consulta` / `ExpedienteClinico` constructors |
| Business  | `fecha` not null                                     | `Consulta` constructor            |

## Exceptions

| Domain exception                  | HTTP |
|-----------------------------------|------|
| `ExpedienteNotFoundException`     | 404  |
| `ConsultaNotFoundException`       | 404  |
| `InvalidHospitalDataException`    | 400  |
| `PatientNotFoundException` (propagated from `patient` feature) | 404 |

## Key technical decisions

### 1. `Gateway` instead of `Repository`

Per [ADR 0002](../../architecture/decisions/0002-gateway-vs-repository.md): MedSync does not own the hospital schema.

### 2. `id` as `String` (varchar), not `UUID`

The hospital defines its own ids as `VARCHAR(50)`. We respect the existing type. When auto-creating a row, we use `UUID.randomUUID().toString()` (canonical form, compatible with `varchar`).

### 3. Two REST resource classes

`PatientResource` already owns `@Path("/api/patients")` (13 literal chars). If `HospitalResource` claimed `@Path("/api")`, JAX-RS would prefer `PatientResource` for `/api/patients/{id}/expediente` because it is more specific by literal-character match — then return 404 because that sub-path is not defined there. Fix: `HospitalResource` claims `@Path("/api/patients/{id}")` (14 literal chars). The `/api/consultas/{consultaId}` endpoint lives in a separate class `HospitalConsultaResource`.

### 4. No `@SQLRestriction`

The hospital owns the lifecycle of its records. MedSync only reads (plus creates `consulta` + auto-creates `expediente`). There is no "soft-deleted expediente" concept here.

### 5. JPQL with subquery, no `@ManyToOne`

Explicit JPQL > object navigation. The hospital data does not require dense graphs. Subqueries are clearer and avoid hidden joins.

### 6. Auto-create `expediente` on first `consulta`

The original plan was "read-only on expedientes". During implementation we relaxed this: if a doctor creates a consulta and there's no expediente, MedSync silently creates one (using only `pacienteExternoId`).

**Justification**: from the client's perspective the expediente is an implicit container — every patient "has" one by definition. Forcing an explicit creation before the first consulta is unjustified friction.

**Risk**: if the hospital later requires its own creation metadata (e.g. assigned doctor), we are bypassing it. Flagged for review with the hospital team.

**Reversal cost**: trivial — replace `.orElseGet(...)` with `.orElseThrow(ExpedienteNotFoundException::new)` in `CreateConsultaService`.

### 7. Hibernate `drop-and-create` in `%test`, `validate` in `%mysql-local`

- `%dev`, `%test`: H2 in-memory, `drop-and-create`. Hibernate generates the hospital schema from `@Entity` classes. No Docker, no Flyway for hospital.
- `%mysql-local`, `%prod`: `validate`. Schema comes from `docker/hospital-initdb/01_schema.sql`. Hibernate validates the match.

**Trade-off**: tests don't validate MySQL-specific syntax for the hospital schema. We accept this because we only use portable JPQL. If we add MySQL-only native queries later, we promote tests to Testcontainers.

### 8. Independent transactions per datasource

No XA. If a flow reads from MedSync and writes to the hospital and the second step fails, the read already happened. Accepted because only the consulta create writes to the hospital — and the read of the patient in MedSync has no side effect.

### 9. Client supplies `fecha` (ISO-8601 `LocalDateTime`)

The server does not infer "now". Doctors can register retroactive consultas (as long as DTO and domain validation pass).

## Sequence: Create consulta (with auto-create expediente)

```
Client      HospitalResource     CreateConsultaService     PatientRepository    HospitalGateway     MedSync DB    Hospital DB
  │  POST   │                    │                         │                    │                   │             │
  │ ───────►│ @Valid req         │                         │                    │                   │             │
  │         │ execute(patientId, req)                      │                    │                   │             │
  │         │ ──────────────────►│                         │                    │                   │             │
  │         │                    │ patient ← findByUuid    │                    │                   │             │
  │         │                    │ ───────────────────────►│ SELECT             │                   │             │
  │         │                    │                         │ ──────────────────►│                   │             │
  │         │                    │ exp ← findExpedienteBy  │                    │                   │             │
  │         │                    │ ─────────────────────────────────────────►   │ SELECT            │             │
  │         │                    │                         │                    │ ─────────────────────────────► │
  │         │                    │ exp is empty:           │                    │                   │             │
  │         │                    │   exp ← saveExpediente(...) ────────────────►│ INSERT expediente │             │
  │         │                    │                         │                    │ ─────────────────────────────► │
  │         │                    │ consulta ← saveConsulta(...)               ──►│ INSERT consulta │             │
  │         │                    │                         │                    │ ─────────────────────────────► │
  │         │ ◄── Consulta ──────│                         │                    │                   │             │
  │ ◄── 201 ────────────────────│                         │                    │                   │             │
```
