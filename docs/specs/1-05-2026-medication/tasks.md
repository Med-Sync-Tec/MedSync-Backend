# Feature: Medication — Implementation Tasks

> Status: **all tasks complete**. Historical record.

## Domain
- [x] `MedicamentoTest`, `MedicamentoEstadoTest` → RED
- [x] `Medicamento`, `MedicamentoEstado` POJOs
- [x] `MedicamentoEstadoNames`, `MedicamentoWithEstado`, `MedicamentosPage`
- [x] `MedicamentoRepository`, `MedicamentoEstadoRepository` interfaces
- [x] 6 `*UseCase` interfaces
- [x] Exceptions: `MedicamentoNotFoundException`, `DuplicateMedicamentoException`, `InvalidMedicamentoDataException`, `EstadoNotFoundException`

## Application
- [x] 6 `*ServiceTest` (Mockito) → RED
- [x] 6 `*Service` implementations
- [x] Authenticated-user check helper

## Persistence
- [x] Migration `V7__create_medicamentos_schema.sql`
- [x] Migration `V8__seed_medicamentos.sql` (3 estados + 15 medications)
- [x] `MedicamentoRepositoryImplTest`, `MedicamentoEstadoRepositoryImplTest` → RED
- [x] Entities + `@NamedEntityGraph("Medicamento.withEstado")`
- [x] Persistence mappers
- [x] Repository implementations (manual pagination)

## REST
- [x] Integration tests covering all endpoints + status codes + auth gating → RED
- [x] DTOs: `CreateMedicamentoRequest`, `UpdateMedicamentoRequest`, `UpdateEstadoRequest`, `MedicamentoResponse`, `MedicamentosPageResponse`
- [x] `MedicamentoRestMapper`
- [x] `MedicamentoResource`
- [x] Wire 4 new exceptions into `GlobalExceptionHandler`

## Verification
- [x] `./mvnw test` green
- [x] `./mvnw verify -DskipITs=false` green
- [x] Swagger UI lists all 6 endpoints under `Medications`
- [x] Manual smoke: list returns 15 seeded medications across 3 statuses
