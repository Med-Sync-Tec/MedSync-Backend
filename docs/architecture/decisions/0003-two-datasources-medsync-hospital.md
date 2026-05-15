# ADR 0003: Two MySQL Datasources (MedSync + Hospital)

- **Status**: Accepted
- **Date**: 2026-04-26
- **Deciders**: Backend team

## Context

MedSync must read clinical records (`expedientes_clinicos`, `consultas`) that live in a **separate database owned by the hospital**. MedSync cannot:

- Modify the hospital schema.
- Run migrations there.
- Assume the hospital DB is on the same MySQL instance as ours.

We also need MedSync to operate as a standalone product (without a hospital connection) for local development, demos, and testing.

Options considered:

1. **Single datasource, hospital tables imported on demand** — denormalized copy in our DB, synced periodically. Rejected: stale data, complex sync logic, defeats the point.
2. **Hospital data via HTTP API** — the hospital exposes a REST API, we consume it. Rejected: hospital has no such API; building it is out of scope.
3. **Two datasources, separate persistence units** — Quarkus configures both, each with its own Hibernate session. Selected.

## Decision

Quarkus is configured with **two named datasources**:

- `<default>` — MedSync's own MySQL DB. Owns: `patients`, `users`, `articles`, `medicamentos`, `paciente_contexto`, future `alerts`. Schema managed by **Flyway** (`src/main/resources/db/migration/`). Hibernate runs in `validate` mode.
- `hospital` — external hospital MySQL DB. Read-mostly. Owns: `expedientes_clinicos`, `consultas`. Schema **not** managed by us — defined in `docker/hospital-initdb/01_schema.sql` for local sims, supplied by the hospital in production. Hibernate runs in `validate` mode against real MySQL, `drop-and-create` against H2 in tests.

Each datasource has a separate persistence unit, and `@Entity` classes are split into disjoint packages:

- `itesm.medsync.infrastructure.persistence.*` → `<default>` PU
- `itesm.medsync.infrastructure.hospital.*` → `hospital` PU

Transactions are **independent per datasource**. We do **not** use XA / two-phase commit.

## Consequences

### Positive

- Clear ownership boundary: MedSync never accidentally writes to the hospital schema (except the explicit auto-create of `expedientes_clinicos` documented in the hospital spec).
- We can swap the hospital backend later (HTTP, gRPC, another DB) by reimplementing `HospitalGateway` — domain and application layers don't change.
- Local development works without a hospital container — Hibernate `drop-and-create` on H2 regenerates the hospital schema from `@Entity`s.

### Negative

- **No cross-database transactions**. If a flow reads from MedSync and writes to the hospital and the second step fails, the first read has already happened. Accepted because:
  - The only hospital write today is creating a `consulta` (and auto-creating an `expediente_clinico`). Both are idempotent enough that a retry recovers.
  - Read-write across boundaries is rare by design.
- **Two MySQL containers** in dev. Slightly heavier docker-compose. Documented in [stack.md](../stack.md).
- **JDBC connection failures to the hospital** currently surface as a generic 500. Mapping to 503 is acknowledged technical debt — see hospital spec.

### Neutral

- Tests run against H2 in `MODE=MySQL` for both datasources. JPQL is portable; native MySQL-specific syntax would force us to switch to Testcontainers.

## Configuration excerpt

```properties
# Default DS (MedSync)
quarkus.datasource.db-kind=mysql
quarkus.datasource.jdbc.url=jdbc:mysql://localhost:3307/medsync
quarkus.hibernate-orm.packages=itesm.medsync.infrastructure.persistence
quarkus.hibernate-orm.database.generation=validate
quarkus.flyway.migrate-at-start=true

# Hospital DS (external)
quarkus.datasource.hospital.db-kind=mysql
quarkus.datasource.hospital.jdbc.url=jdbc:mysql://localhost:3308/hospital
quarkus.hibernate-orm.hospital.datasource=hospital
quarkus.hibernate-orm.hospital.packages=itesm.medsync.infrastructure.hospital
quarkus.hibernate-orm.hospital.database.generation=validate
```

## Related decisions

- [ADR 0002 — Gateway vs Repository](0002-gateway-vs-repository.md)
