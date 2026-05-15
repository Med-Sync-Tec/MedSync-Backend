# Naming Conventions

## Package names

- All packages lowercase, single word per segment: `itesm.medsync.domain.patient`.
- Feature names in **English, singular**: `patient`, `article`, `user`, `medication`, `alert`.
- One word preferred. Hyphens not allowed in Java packages — use camelCase only if necessary (`patientContext`).

### Legacy Spanish packages

The following packages still use Spanish identifiers and are documented for English in the specs. Renaming is deferred (high churn, large diff):

| Current (Java)     | Target (English)  | Spec name          |
|--------------------|-------------------|--------------------|
| `medicamento`      | `medication`      | `medication`       |
| `pacientecontexto` | `patientContext`  | `patient-context`  |

When working in these packages, keep using their existing Spanish identifiers — do **not** mix English and Spanish in the same file. The English rename happens in a single dedicated refactor PR.

## Class names

### Domain layer (`domain/<feature>/`)

| Type             | Pattern                       | Example                                |
|------------------|-------------------------------|----------------------------------------|
| Aggregate / entity model | `<Feature>`           | `Patient`, `Article`                   |
| Value object     | `<Feature><Concept>`          | `ArticleTag`, `MedicamentoEstado`      |
| Use case interface | `<Action><Feature>UseCase`  | `CreatePatientUseCase`, `ListArticlesUseCase` |
| Repository (own data) | `<Feature>Repository`    | `PatientRepository`, `UserRepository`  |
| Gateway (external) | `<System>Gateway`           | `HospitalGateway`                      |
| Exception        | `<Concept>Exception`          | `PatientNotFoundException`, `DuplicateArticleException`, `InvalidPatientDataException` |

### Application layer (`application/<feature>/`)

| Type             | Pattern                       | Example                                |
|------------------|-------------------------------|----------------------------------------|
| Service          | `<Action><Feature>Service`    | `CreatePatientService`, `SyncPubmedArticlesService` |

One service implements exactly one use case interface — do not group multiple use cases in one service.

### Infrastructure layer (`infrastructure/persistence/<feature>/`)

| Type             | Pattern                              | Example                       |
|------------------|--------------------------------------|-------------------------------|
| JPA entity       | `<Feature>Entity`                    | `PatientEntity`               |
| Panache repo impl | `<Feature>RepositoryImpl`           | `PatientRepositoryImpl`       |
| Persistence mapper | `<Feature>PersistenceMapper`       | `PatientPersistenceMapper`    |

`<Feature>RepositoryImpl` implements **both** the domain `<Feature>Repository` interface and `PanacheRepository<<Feature>Entity>` in the same class. Do not create a separate `<Feature>PanacheRepository` file.

### Interfaces layer (`interfaces/rest/<feature>/`)

| Type             | Pattern                       | Example                       |
|------------------|-------------------------------|-------------------------------|
| Resource (controller) | `<Feature>Resource`      | `PatientResource`             |
| Request DTO      | `<Action><Feature>Request`    | `CreatePatientRequest`, `UpdateMedicamentoRequest` |
| Response DTO     | `<Feature>Response` or `<Concept>Response` | `PatientResponse`, `ArticleTagResponse` |
| REST mapper      | `<Feature>RestMapper`         | `PatientRestMapper`           |

## Method names

- Repository read methods: `findBy<Field>` returning `Optional<T>` or `List<T>`. Use `findByUuid(UUID)` when the JPA `findById` conflicts with the domain method signature (see hospital spec).
- Repository write methods: `save(T)`, `delete(T)` or `deleteById(...)`.
- Existence checks: `existsBy<Field>(...)` returning `boolean`.
- Use case interfaces have a single method: `execute(...)`.
- REST resource methods: imperative verbs that describe the operation: `createPatient`, `listPatients`, `getPatientById`.

## DTO field naming

- Request DTOs: `public` fields (Jakarta Validation friendly), camelCase, matching the JSON shape we accept from the client.
- Response DTOs: Java `record` with camelCase components.
- Never reuse the domain model as a DTO. Always go through a `RestMapper`.

## File naming

One public class per file. File name matches the class name. Test files end with `Test` (unit) or `IT` (integration, `@QuarkusTest` + RestAssured).

## Database identifiers

- Tables: snake_case, plural: `patients`, `articulos_cientificos`, `medicamentos`.
- Columns: snake_case: `expediente_externo_id`, `created_at`.
- Primary keys: always named `id`, type `BINARY(16)` for UUIDs.
- Foreign keys: `<referenced_table_singular>_id`: `medico_id`, `articulo_id`.
- Indexes: `idx_<table>_<columns>`: `idx_patients_medico`.
- Constraints: `pk_<table>`, `uq_<table>_<columns>`, `fk_<table>_<referenced>`.

## REST paths

- Plural collection nouns, kebab-case if multiple words: `/api/patients`, `/api/articles`, `/api/matching-articles`.
- Nested resources reflect the parent: `/api/patients/{id}/contextos`, `/api/patients/{id}/consultas`.
- Path parameters use the same name as the resource: `{patientId}`, `{articleId}`. Use a generic `{id}` only when the resource is unambiguous from the path.
