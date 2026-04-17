package itesm.medsync.infrastructure.config;

import itesm.medsync.domain.patient.exception.DuplicatePatientException;
import itesm.medsync.domain.patient.exception.InvalidPatientDataException;
import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;

public final class GlobalExceptionHandler {

    private GlobalExceptionHandler() {
    }

    @Provider
    public static class PatientNotFoundMapper implements ExceptionMapper<PatientNotFoundException> {
        @Override
        public Response toResponse(PatientNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(404, "Not Found", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class DuplicatePatientMapper implements ExceptionMapper<DuplicatePatientException> {
        @Override
        public Response toResponse(DuplicatePatientException ex) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(409, "Conflict", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class InvalidPatientDataMapper implements ExceptionMapper<InvalidPatientDataException> {
        @Override
        public Response toResponse(InvalidPatientDataException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(400, "Bad Request", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {
        @Override
        public Response toResponse(ConstraintViolationException ex) {
            List<ErrorResponse.FieldError> details = ex.getConstraintViolations().stream()
                    .map(v -> new ErrorResponse.FieldError(
                            lastPathSegment(v.getPropertyPath().toString()),
                            v.getMessage()))
                    .toList();
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(400, "Bad Request", "Validation failed", details))
                    .build();
        }

        private static String lastPathSegment(String path) {
            int idx = path.lastIndexOf('.');
            return idx < 0 ? path : path.substring(idx + 1);
        }
    }

    @Provider
    public static class PersistenceUniqueViolationMapper implements ExceptionMapper<org.hibernate.exception.ConstraintViolationException> {
        @Override
        public Response toResponse(org.hibernate.exception.ConstraintViolationException ex) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(409, "Conflict", "Constraint violation: " + ex.getConstraintName()))
                    .build();
        }
    }

    @Provider
    public static class WebApplicationMapper implements ExceptionMapper<WebApplicationException> {
        @Override
        public Response toResponse(WebApplicationException ex) {
            Response r = ex.getResponse();
            return Response.status(r.getStatus())
                    .entity(new ErrorResponse(r.getStatus(),
                            Response.Status.fromStatusCode(r.getStatus()) != null
                                    ? Response.Status.fromStatusCode(r.getStatus()).getReasonPhrase()
                                    : "Error",
                            ex.getMessage() != null ? ex.getMessage() : "Request error"))
                    .build();
        }
    }

    @Provider
    public static class FallbackMapper implements ExceptionMapper<Throwable> {
        private static final Logger LOG = Logger.getLogger(FallbackMapper.class);

        @Override
        public Response toResponse(Throwable ex) {
            if (ex instanceof WebApplicationException wae) {
                return new WebApplicationMapper().toResponse(wae);
            }
            LOG.error("Unhandled exception", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(500, "Internal Server Error", "Unexpected error"))
                    .build();
        }
    }
}
