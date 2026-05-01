package itesm.medsync.infrastructure.config;

import itesm.medsync.domain.hospital.exception.ConsultaNotFoundException;
import itesm.medsync.domain.hospital.exception.ExpedienteNotFoundException;
import itesm.medsync.domain.hospital.exception.InvalidHospitalDataException;
import itesm.medsync.domain.pacientecontexto.exception.InvalidPacienteContextoDataException;
import itesm.medsync.domain.pacientecontexto.exception.PacienteContextoNotFoundException;
import itesm.medsync.domain.patient.exception.DuplicatePatientException;
import itesm.medsync.domain.patient.exception.InvalidPatientDataException;
import itesm.medsync.domain.patient.exception.PatientNotFoundException;
import itesm.medsync.domain.medicamento.exception.DuplicateMedicamentoException;
import itesm.medsync.domain.medicamento.exception.EstadoNotFoundException;
import itesm.medsync.domain.medicamento.exception.InvalidMedicamentoDataException;
import itesm.medsync.domain.medicamento.exception.MedicamentoNotFoundException;
import itesm.medsync.domain.user.exception.InvalidUserDataException;
import itesm.medsync.domain.user.exception.RoleMismatchException;
import itesm.medsync.domain.user.exception.RoleNotFoundException;
import itesm.medsync.domain.user.exception.UserAlreadyExistsException;
import itesm.medsync.domain.user.exception.UserNotFoundException;
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
    public static class ExpedienteNotFoundMapper implements ExceptionMapper<ExpedienteNotFoundException> {
        @Override
        public Response toResponse(ExpedienteNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(404, "Not Found", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class ConsultaNotFoundMapper implements ExceptionMapper<ConsultaNotFoundException> {
        @Override
        public Response toResponse(ConsultaNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(404, "Not Found", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class InvalidHospitalDataMapper implements ExceptionMapper<InvalidHospitalDataException> {
        @Override
        public Response toResponse(InvalidHospitalDataException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(400, "Bad Request", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class MedicamentoNotFoundMapper implements ExceptionMapper<MedicamentoNotFoundException> {
        @Override
        public Response toResponse(MedicamentoNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(404, "Not Found", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class DuplicateMedicamentoMapper implements ExceptionMapper<DuplicateMedicamentoException> {
        @Override
        public Response toResponse(DuplicateMedicamentoException ex) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(409, "Conflict", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class InvalidMedicamentoDataMapper implements ExceptionMapper<InvalidMedicamentoDataException> {
        @Override
        public Response toResponse(InvalidMedicamentoDataException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(400, "Bad Request", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class EstadoNotFoundMapper implements ExceptionMapper<EstadoNotFoundException> {
        @Override
        public Response toResponse(EstadoNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(404, "Not Found", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class UserAlreadyExistsMapper implements ExceptionMapper<UserAlreadyExistsException> {
        @Override
        public Response toResponse(UserAlreadyExistsException ex) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(409, "Conflict", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class UserNotFoundMapper implements ExceptionMapper<UserNotFoundException> {
        @Override
        public Response toResponse(UserNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(404, "Not Found", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class InvalidUserDataMapper implements ExceptionMapper<InvalidUserDataException> {
        @Override
        public Response toResponse(InvalidUserDataException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(400, "Bad Request", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class RoleMismatchMapper implements ExceptionMapper<RoleMismatchException> {
        private static final Logger LOG = Logger.getLogger(RoleMismatchMapper.class);

        @Override
        public Response toResponse(RoleMismatchException ex) {
            LOG.debugf("Role mismatch: actual=%s expected=%s", ex.getActualRole(), ex.getExpectedRole());
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new RoleMismatchErrorResponse(
                            403,
                            "Forbidden",
                            ex.getMessage(),
                            ex.getExpectedRole()))
                    .build();
        }
    }

    @Provider
    public static class RoleNotFoundMapper implements ExceptionMapper<RoleNotFoundException> {
        @Override
        public Response toResponse(RoleNotFoundException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(400, "Bad Request", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class PacienteContextoNotFoundMapper implements ExceptionMapper<PacienteContextoNotFoundException> {
        @Override
        public Response toResponse(PacienteContextoNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(404, "Not Found", ex.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class InvalidPacienteContextoDataMapper implements ExceptionMapper<InvalidPacienteContextoDataException> {
        @Override
        public Response toResponse(InvalidPacienteContextoDataException ex) {
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
