# Feature: Hospital — Implementation Tasks

> Status: **all tasks complete**. Historical record.

## Infra (no TDD, setup)
- [x] `docker/hospital-initdb/01_schema.sql` — schema for `expedientes_clinicos` + `consultas`
- [x] `docker-compose.yml` — add `hospital-db` service (port 3308) + volume + healthcheck
- [x] `application.properties` — add datasource + persistence unit `hospital`; lock default PU to `infrastructure.persistence.*`
- [x] Verify `docker compose up -d` brings both DBs healthy and Quarkus boots without `validate` errors

## Domain
- [x] `ExpedienteClinicoTest`, `ConsultaTest` → RED
- [x] `ExpedienteClinico`, `Consulta` POJOs + factories
- [x] `ExpedienteNotFoundException`, `ConsultaNotFoundException`, `InvalidHospitalDataException`
- [x] `HospitalGateway` interface
- [x] 4 `*UseCase` interfaces

## Application
- [x] `GetExpedienteByPatientServiceTest`, `GetConsultasByPatientServiceTest`, `GetConsultaByIdServiceTest`, `CreateConsultaServiceTest` (Mockito) → RED
- [x] Implementations → GREEN

## Infrastructure
- [x] `HospitalGatewayImplTest` (`@QuarkusTest` + `@TestTransaction` against H2 hospital PU) → RED
- [x] `ExpedienteClinicoHospitalEntity`, `ConsultaHospitalEntity`
- [x] `HospitalPersistenceMapper`
- [x] `HospitalGatewayImpl` with `@PersistenceContext(unitName="hospital")`

## REST
- [x] `HospitalResourceIT` covering 14 scenarios (GETs, POST with/without expediente, 400, 404) → RED
- [x] DTOs (`CreateConsultaRequest`, `ExpedienteClinicoResponse`, `ConsultaResponse`)
- [x] `HospitalRestMapper`
- [x] `HospitalResource` (`@Path("/api/patients/{id}")`) + `HospitalConsultaResource` (`@Path("/api/consultas")`)
- [x] Wire 3 new exceptions into `GlobalExceptionHandler`

## Verification
- [x] `./mvnw compile` clean
- [x] `./mvnw test` green (unit + gateway integration)
- [x] `./mvnw verify -DskipITs=false` green (REST IT)
- [x] Swagger UI lists all 4 endpoints under `Hospital`
