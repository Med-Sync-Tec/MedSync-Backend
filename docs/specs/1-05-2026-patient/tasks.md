# Feature: Patient — Implementation Tasks

> Status: **all tasks complete**. Preserved as a historical record of the implementation order.

## Domain
- [x] `PatientTest` (constructor invariants, factory, `softDelete`, `equals`/`hashCode`) → RED
- [x] `Patient` model + `InvalidPatientDataException` → GREEN
- [x] `PatientRepository` interface
- [x] `CreatePatientUseCase`, `GetPatientByIdUseCase`, `ListActivePatientsUseCase`, `DeletePatientUseCase`
- [x] `PatientNotFoundException`, `DuplicatePatientException`

## Application
- [x] `CreatePatientServiceTest` (duplicate, happy path) → RED
- [x] `CreatePatientService` → GREEN
- [x] `GetPatientByIdServiceTest`, `GetPatientByIdService`
- [x] `ListActivePatientsServiceTest`, `ListActivePatientsService`
- [x] `DeletePatientServiceTest`, `DeletePatientService`

## Persistence
- [x] Migration `V1__create_patients_table.sql`
- [x] `PatientRepositoryImplTest` (`@SQLRestriction` filtering, native query, save/update) → RED
- [x] `PatientEntity` + `@SQLRestriction("activo = true")`
- [x] `PatientPersistenceMapper`
- [x] `PatientRepositoryImpl` implements `PatientRepository` + `PanacheRepository<PatientEntity>`

## REST
- [x] `PatientResourceIT` covering POST/GET list/GET by id/DELETE + all status codes → RED
- [x] `CreatePatientRequest` with Jakarta Validation
- [x] `PatientResponse` record
- [x] `PatientRestMapper`
- [x] `PatientResource` (`@Path("/api/patients")`)
- [x] Wire `PatientNotFoundException`, `DuplicatePatientException`, `InvalidPatientDataException` into `GlobalExceptionHandler`

## Verification
- [x] `./mvnw compile` clean
- [x] `./mvnw test` green
- [x] `./mvnw verify -DskipITs=false` green
- [x] Swagger UI lists the four endpoints under `Patient`
