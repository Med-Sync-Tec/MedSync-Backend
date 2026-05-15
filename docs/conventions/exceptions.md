# Exception Conventions

Every feature defines its own domain exceptions under `domain/<feature>/exception/`. The `infrastructure/config/GlobalExceptionHandler` maps them to HTTP status codes — resource methods never catch exceptions themselves.

## Naming pattern

| Concept                             | Class name                       | HTTP status |
|-------------------------------------|----------------------------------|-------------|
| Resource not found                  | `<Resource>NotFoundException`    | `404`       |
| Duplicate / conflict                | `Duplicate<Resource>Exception`   | `409`       |
| Domain limit exceeded               | `<Resource>LimitExceededException` | `409`     |
| Invalid input (domain invariants)   | `Invalid<Resource>DataException` | `400`       |
| Mismatch between expected and actual | `<Concept>MismatchException`    | `403` or `400` (decide per case) |
| Unauthorized access                 | `Unauthorized<Concept>Exception` | `401` / `403` |
| Anything not enumerated above       | (no specific exception) — falls through | `500` |

## Status code mapping

The `GlobalExceptionHandler` lives in `infrastructure/config/`. Every new exception must be wired there:

```java
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable ex) {
        return switch (ex) {
            case PatientNotFoundException e        -> error(404, "PATIENT_NOT_FOUND", e.getMessage());
            case ExpedienteNotFoundException e     -> error(404, "EXPEDIENTE_NOT_FOUND", e.getMessage());
            case ConsultaNotFoundException e       -> error(404, "CONSULTA_NOT_FOUND", e.getMessage());
            case ArticleNotFoundException e        -> error(404, "ARTICLE_NOT_FOUND", e.getMessage());
            case UserNotFoundException e           -> error(404, "USER_NOT_FOUND", e.getMessage());

            case DuplicatePatientException e       -> error(409, "DUPLICATE_PATIENT", e.getMessage());
            case DuplicateArticleException e       -> error(409, "DUPLICATE_ARTICLE", e.getMessage());
            case UserAlreadyExistsException e      -> error(409, "DUPLICATE_USER", e.getMessage());
            case ConstraintViolationException e    -> validationError(e);   // Hibernate, race-condition catch

            case InvalidPatientDataException e     -> error(400, "INVALID_PATIENT_DATA", e.getMessage());
            case InvalidArticleDataException e     -> error(400, "INVALID_ARTICLE_DATA", e.getMessage());
            case jakarta.validation.ConstraintViolationException e  -> validationError(e);

            case RoleMismatchException e           -> error(403, "ROLE_MISMATCH", e.getMessage());

            default -> {
                Log.error("Unhandled exception", ex);
                yield error(500, "INTERNAL_ERROR", "An unexpected error occurred");
            }
        };
    }
}
```

(Exact code structure may vary — what matters is the mapping.)

## Response shape

All error responses share this JSON envelope:

```json
{
  "error": "PATIENT_NOT_FOUND",
  "message": "Patient with id '...' was not found",
  "details": null
}
```

For validation errors, `details` is a list of field errors:

```json
{
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "details": [
    { "field": "fechaNacimiento", "message": "must be a past date" },
    { "field": "nombre", "message": "must not be blank" }
  ]
}
```

## Rules

- **Never let a stack trace reach the client.** The default branch in the handler returns a generic 500 message and logs the trace server-side.
- **Never throw `IllegalArgumentException` from the domain.** Use a feature-specific `Invalid<Feature>DataException`.
- **Don't catch in the resource.** The whole point of the global handler is centralization. Resource methods stay clean.
- **One exception class per failure mode** — don't reuse a generic exception for both "not found" and "invalid input". The HTTP mapping needs to distinguish them.
- **Constructor message lives in the exception.** When throwing, pass the offending value (id, field name) in the message. The handler reads `e.getMessage()`.

## When to add a new exception

Ask: does this failure correspond to a distinct HTTP response? If yes, add a specific exception. If no (e.g. "name too long" — already covered by validation), reuse `Invalid<Feature>DataException`.

## Hospital connection failures

Currently `jakarta.persistence.PersistenceException` and underlying JDBC exceptions from the `hospital` datasource fall through to the default 500. Mapping them to `503 Service Unavailable` is acknowledged technical debt — tracked in [specs/1-05-2026-hospital/summary.md](../specs/1-05-2026-hospital/summary.md).
