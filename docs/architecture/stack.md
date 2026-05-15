# Technology Stack

## Runtime

| Component       | Choice                     | Version    | Notes                                            |
|-----------------|----------------------------|------------|--------------------------------------------------|
| Language        | Java                       | 21         | Records, pattern matching, virtual threads ready |
| Framework       | Quarkus                    | 3.34.3     | Native image–capable, fast startup               |
| Build           | Maven                      | wrapper    | `./mvnw` — pinned wrapper, no global Maven       |
| Container       | Docker / docker-compose    | —          | Two MySQL services (medsync-db, hospital-db)     |

## Data layer

| Component       | Choice                     | Profile                       |
|-----------------|----------------------------|-------------------------------|
| Primary DB      | MySQL 8.0                  | `%prod`, `%mysql-local`       |
| Dev / test DB   | H2 in `MODE=MySQL`         | `%dev`, `%test`               |
| ORM             | Hibernate ORM with Panache | all                           |
| Migrations      | Flyway                     | `<default>` datasource only   |
| Schema strategy | `validate` against Flyway  | Never `update` or `create`    |

The `hospital` datasource runs in `validate` mode against a schema owned by the external hospital. In `%dev` / `%test`, Hibernate uses `drop-and-create` on H2 (the hospital schema is regenerated from `@Entity` classes). In `%mysql-local` / `%prod`, the schema comes from `docker/hospital-initdb/01_schema.sql`.

## Web layer

| Component       | Choice                              |
|-----------------|-------------------------------------|
| REST framework  | Quarkus REST (Resteasy Reactive)    |
| JSON            | Jackson                             |
| Validation      | Hibernate Validator (Jakarta Bean Validation) |
| OpenAPI         | SmallRye OpenAPI + Swagger UI       |

Swagger UI: `http://localhost:8080/q/swagger-ui`
OpenAPI JSON: `http://localhost:8080/q/openapi`

## Testing

| Layer            | Tool                                     |
|------------------|------------------------------------------|
| Unit tests       | JUnit 5 (Jupiter)                        |
| Mocking          | Mockito                                  |
| Integration      | `@QuarkusTest` + `@TestTransaction`      |
| REST integration | `@QuarkusTest` + RestAssured             |
| Coverage         | Jacoco (target ≥80% on domain + application) |

See [conventions/testing.md](../conventions/testing.md).

## External integrations

| Integration     | Module                                   | Purpose                                |
|-----------------|------------------------------------------|----------------------------------------|
| PubMed E-utils  | `infrastructure/pubmed/`                 | Scheduled sync of scientific articles  |
| Hospital DB     | `infrastructure/hospital/` (separate PU) | Clinical records (expedientes, consultas) |
| AI tagging      | `infrastructure/ai/` *(reserved)*        | Article tag extraction (not yet wired) |

## Operations

| Concern         | Tool / approach                                 |
|-----------------|-------------------------------------------------|
| Container build | Multi-stage Dockerfile (JVM mode)               |
| CI / Build      | Google Cloud Build (`cloudbuild.yaml`)          |
| Local dev       | `docker-compose.yml` + `./mvnw quarkus:dev`     |
| Profiles        | `%dev`, `%test`, `%mysql-local`, `%prod`        |

## Useful commands

```bash
./mvnw quarkus:dev                                      # live reload, H2 in-memory
./mvnw quarkus:dev -Dquarkus.profile=mysql-local        # against docker MySQL
./mvnw test                                             # unit + integration (no IT)
./mvnw verify -DskipITs=false                           # include REST IT
./mvnw package                                          # JVM jar in target/
./mvnw package -Dnative                                 # native image (requires GraalVM)
./mvnw package -Dnative -Dquarkus.native.container-build=true   # native via container
```

## Why these choices

- **Quarkus over Spring Boot**: faster startup, lower memory, native image option, supervised by the same patterns (Panache ≈ Spring Data).
- **Panache over plain JPA**: removes ~30% of repository boilerplate without hiding JPA when needed.
- **Flyway over Liquibase**: SQL-first migrations are easier to review and debug; team is already SQL-fluent.
- **H2 with `MODE=MySQL` for tests**: tests run without Docker. Trade-off documented — see [ADR 0004](decisions/0004-h2-mysql-mode-for-tests.md) *(planned)*.
