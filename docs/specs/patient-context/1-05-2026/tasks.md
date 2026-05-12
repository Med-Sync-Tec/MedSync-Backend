# Feature: Patient Context — Implementation Tasks

> Status: **all tasks complete**. Historical record.

## Shared (if not already present)
- [x] `TipoClinico` enum in `domain/shared/model/`

## Domain
- [x] `PacienteContextoTest` (invariants, factory) → RED
- [x] `PacienteContexto` POJO
- [x] Exceptions: `PacienteContextoNotFoundException`, `InvalidPacienteContextoDataException`
- [x] `PacienteContextoRepository` interface
- [x] 3 `*UseCase` interfaces

## Application
- [x] `AddPacienteContextoServiceTest`, `ListPacienteContextosByPacienteServiceTest`, `DeletePacienteContextoServiceTest` → RED
- [x] 3 `*Service` implementations
- [x] Ownership-mismatch returns 404 (security decision documented)

## Persistence
- [x] Migration `V5__create_paciente_contexto_table.sql` (table + 2 indexes + FK to `patients(id)`)
- [x] `PacienteContextoRepositoryImplTest` → RED
- [x] `PacienteContextoEntity` (`@Enumerated(STRING)` on `tipo`)
- [x] `PacienteContextoPersistenceMapper`
- [x] `PacienteContextoRepositoryImpl` (Panache)

## REST
- [x] `PacienteContextoResourceIT` covering all 3 endpoints + ownership-mismatch case → RED
- [x] DTOs: `AddPacienteContextoRequest`, `PacienteContextoResponse`
- [x] `PacienteContextoRestMapper`
- [x] `PacienteContextoResource` (`@Path("/api/patients/{patientId}/contextos")`)
- [x] Wire 2 new exceptions into `GlobalExceptionHandler`

## Verification
- [x] `./mvnw test` green
- [x] `./mvnw verify -DskipITs=false` green
- [x] Cross-feature: `getMatchingArticlesByPatient` joins against `paciente_contexto` and returns expected articles in integration test
