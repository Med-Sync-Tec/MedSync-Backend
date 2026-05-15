# Testing Conventions

## Goal

≥80% line coverage on `domain/` and `application/` layers. Lower bar on `infrastructure/` and `interfaces/` (integration tests cover the wiring).

## Test pyramid

```
                ┌─────────────────────────┐
                │   REST integration ITs  │   slowest, fewest, end-to-end
                │     @QuarkusTest +      │
                │       RestAssured       │
                ├─────────────────────────┤
                │ Persistence integration │   medium, per repository
                │     @QuarkusTest +      │
                │     @TestTransaction    │
                ├─────────────────────────┤
                │   Service unit tests    │   fast, per service
                │   JUnit 5 + Mockito     │
                ├─────────────────────────┤
                │   Domain unit tests     │   fastest, most numerous
                │      JUnit 5 pure       │
                └─────────────────────────┘
```

## Domain layer — pure JUnit

No frameworks, no Quarkus boot. Tests run in milliseconds.

```java
class PatientTest {

    @Test
    void rejectsFutureBirthDate() {
        assertThatThrownBy(() -> new Patient(
                UUID.randomUUID(),
                "expediente-1",
                "Juan",
                LocalDate.now().plusDays(1),    // future
                "M",
                UUID.randomUUID(),
                true, null, null))
            .isInstanceOf(InvalidPatientDataException.class)
            .hasMessageContaining("fechaNacimiento");
    }
}
```

What to cover:

- Constructor validations (happy + each invariant rejected).
- Factory methods (`Patient.create(...)`).
- State transitions (`patient.softDelete()` returns a new instance).
- `equals` / `hashCode` based on id.

## Application layer — JUnit + Mockito

Mock the repository and any other domain port; assert the service's orchestration.

```java
@ExtendWith(MockitoExtension.class)
class CreatePatientServiceTest {

    @Mock PatientRepository repo;
    @InjectMocks CreatePatientService service;

    @Test
    void rejectsDuplicateExpedienteExternoId() {
        when(repo.existsByExpedienteExternoId("e1")).thenReturn(true);

        assertThatThrownBy(() -> service.execute(/* request */, "e1", /* ... */))
            .isInstanceOf(DuplicatePatientException.class);

        verify(repo, never()).save(any());
    }
}
```

What to cover:

- Happy path → repository called with expected arguments.
- Each business exception thrown.
- Exception propagation from collaborators (e.g. `PatientNotFoundException` flows out of `GetExpedienteByPatientService`).

## Infrastructure layer — `@QuarkusTest`

Use `@QuarkusTest` + `@TestTransaction` so each test rolls back. Tests run against H2 in `MODE=MySQL`.

```java
@QuarkusTest
class PatientRepositoryImplTest {

    @Inject PatientRepositoryImpl repo;

    @Test
    @TestTransaction
    void softDeletedPatientIsHiddenByDefault() {
        Patient patient = repo.save(buildValidPatient());
        repo.save(patient.softDelete());

        assertThat(repo.findAllActive()).isEmpty();
        assertThat(repo.existsByExpedienteExternoId(patient.getExpedienteExternoId())).isTrue();
    }
}
```

What to cover:

- `@SQLRestriction` filtering.
- Native queries that bypass `@SQLRestriction`.
- EntityGraph loading (`getResultList()` size + no `LazyInitializationException`).
- Constraint violations on duplicate inserts.
- Timestamp fields populated by Hibernate.

## REST layer — `@QuarkusTest` + RestAssured

Integration test file names end with `IT` and run on `./mvnw verify -DskipITs=false`.

```java
@QuarkusTest
class PatientResourceIT {

    @Test
    void createReturns201WithLocationHeader() {
        given()
            .contentType(ContentType.JSON)
            .body(validRequest())
        .when()
            .post("/api/patients")
        .then()
            .statusCode(201)
            .header("Location", containsString("/api/patients/"))
            .body("id", notNullValue())
            .body("expedienteExternoId", equalTo("e1"));
    }
}
```

What to cover:

- One test per status code (`200`, `201`, `204`, `400`, `404`, `409`).
- End-to-end happy path for each endpoint.
- Validation failures → `400` with the right field error.
- Boundary cases that crossed layers (e.g. duplicate after soft-delete → `409`).

## TDD workflow

For every feature task:

1. **RED** — write the failing test first. Run it. Confirm it fails for the right reason.
2. **GREEN** — write the minimum code to pass.
3. **REFACTOR** — clean up names, extract helpers, kill duplication. Tests stay green.

Per project preference (see CLAUDE.md): you write the tests, the operator runs them. Don't run `./mvnw test` yourself — present the test file and let them execute.

## When TDD is too heavy

Pure DTO classes, simple mappers, and one-line getters don't need their own tests — they're covered indirectly by service / IT tests. Use judgement, not religion.

## Commands

```bash
./mvnw test                                # unit + repository ITs (no REST IT)
./mvnw verify -DskipITs=false              # everything, including REST ITs
./mvnw test -Dtest=PatientTest             # one class
./mvnw test -Dtest=PatientTest#rejectsFutureBirthDate    # one method
./mvnw test jacoco:report                  # coverage report at target/site/jacoco/index.html
```
