# Validation Conventions

We validate at **two levels**, intentionally duplicating some checks:

| Level     | Where                                  | What                                  |
|-----------|----------------------------------------|---------------------------------------|
| Format    | Request DTO (`interfaces/rest/`)       | HTTP payload shape: required, length, regex, range |
| Business  | Domain model constructor (`domain/`)   | Invariants of the business object     |

The redundancy is **intentional**: the domain does not trust who invokes it. If a future caller bypasses REST (gRPC, scheduled job, test) the domain remains safe.

## Format validation — DTO level

Use Jakarta Bean Validation annotations on Request DTO fields. Fields are `public` (no getters needed; Jakarta scans fields):

```java
public class CreatePatientRequest {

    @NotBlank
    @Size(max = 200)
    public String nombre;

    @NotNull
    @Past
    public LocalDate fechaNacimiento;

    @NotNull
    public UUID medicoId;

    @Size(max = 20)
    public String genero;
}
```

Validation triggers automatically when the resource method parameter is annotated `@Valid`:

```java
@POST
public Response create(@Valid CreatePatientRequest req) { ... }
```

When validation fails, Hibernate Validator throws `ConstraintViolationException`. The [`GlobalExceptionHandler`](../specs/) maps it to `400 Bad Request`.

## Business validation — Domain constructor

The domain POJO validates invariants in its constructor and throws an `Invalid<Feature>DataException`:

```java
public Patient(UUID id, String nombre, LocalDate fechaNacimiento, /* ... */) {
    if (id == null)         throw new InvalidPatientDataException("id is required");
    if (nombre == null || nombre.isBlank())
        throw new InvalidPatientDataException("nombre is required");
    if (fechaNacimiento == null || fechaNacimiento.isAfter(LocalDate.now()))
        throw new InvalidPatientDataException("fechaNacimiento must be in the past");

    long age = ChronoUnit.YEARS.between(fechaNacimiento, LocalDate.now());
    if (age > 150) throw new InvalidPatientDataException("age must be ≤ 150");

    this.id = id;
    this.nombre = nombre;
    // ...
}
```

Rules of thumb for business validation:

- Reject impossible business states (`age > 150`, `fechaNacimiento` in the future, empty required fields).
- Do **not** validate format that is already enforced upstream (length limits, regex patterns) — those are DTO concerns.
- Throw a feature-specific exception (`Invalid<Feature>DataException`) — never `IllegalArgumentException`. The `GlobalExceptionHandler` maps them to `400`.

## Application-level validation

When validation crosses entities (uniqueness, foreign-key existence), it lives in the service:

```java
@Override
public Patient execute(/* request fields */) {
    if (patientRepository.existsByExpedienteExternoId(expedienteExternoId)) {
        throw new DuplicatePatientException(expedienteExternoId);
    }
    Patient patient = new Patient(/* ... */);
    return patientRepository.save(patient);
}
```

These checks are wrapped in `@Transactional`. If a race condition slips past the application check, the database `UNIQUE` constraint catches it and Hibernate's `ConstraintViolationException` is mapped to `409 Conflict` by the global handler.

## When to add a validation

| Check                                     | Lives in                          |
|-------------------------------------------|-----------------------------------|
| "field must not be blank"                 | DTO (`@NotBlank`) + domain        |
| "string ≤ 200 chars"                      | DTO (`@Size(max=200)`)            |
| "email must contain @"                    | DTO (`@Email`) + domain (basic sanity check) |
| "date must be in the past"                | DTO (`@Past`) + domain            |
| "age ≤ 150"                               | domain only (business rule, not format) |
| "expedienteExternoId unique across active and soft-deleted" | service (native query) + DB `UNIQUE` |
| "doi unique"                              | service + DB `UNIQUE`             |
| "role must exist"                         | service (`roleRepository.findByNombre`) |

## Error response shape

`GlobalExceptionHandler` returns this JSON for all validation errors:

```json
{
  "error": "VALIDATION_FAILED",
  "message": "fechaNacimiento must be in the past",
  "details": [
    { "field": "fechaNacimiento", "message": "must be a past date" }
  ]
}
```

(Domain exceptions return a single `message` without `details`.)

See [exceptions.md](exceptions.md) for the full HTTP-status-code mapping.
