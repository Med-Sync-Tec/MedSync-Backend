package itesm.medsync.interfaces.rest.auth;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.solicitud.model.SolicitudAcceso;
import itesm.medsync.domain.solicitud.usecase.CrearSolicitudUseCase;
import itesm.medsync.domain.user.exception.RoleMismatchException;
import itesm.medsync.domain.user.model.KnownRoles;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.interfaces.rest.user.UserRestMapper;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "Login con verificación de rol esperado")
public class AuthResource {

    private static final String DEFAULT_EXPECTED_ROLE = KnownRoles.DOCTOR;

    private final AuthenticatedUserContext userContext;
    private final CrearSolicitudUseCase crearSolicitud;
    private final Mailer mailer;

    @ConfigProperty(name = "medsync.admin.email")
    String adminEmail;

    @ConfigProperty(name = "medsync.app.backend-url", defaultValue = "http://localhost:8080")
    String backendUrl;

    @Inject
    public AuthResource(AuthenticatedUserContext userContext,
                        CrearSolicitudUseCase crearSolicitud,
                        Mailer mailer) {
        this.userContext = userContext;
        this.crearSolicitud = crearSolicitud;
        this.mailer = mailer;
    }

    @POST
    @Path("/login")
    @Operation(summary = "Verifica que el usuario autenticado tenga el rol esperado")
    @APIResponse(responseCode = "200", description = "Login válido, devuelve usuario")
    @APIResponse(responseCode = "401", description = "No autenticado (sin token Firebase)")
    @APIResponse(responseCode = "403", description = "Rol del usuario no coincide con expectedRole")
    public Response login(@Valid LoginRequest request) {
        UserWithRole current = userContext.getCurrentUser();
        if (current == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String expected = (request != null && request.expectedRole != null && !request.expectedRole.isBlank())
                ? request.expectedRole.trim().toUpperCase()
                : DEFAULT_EXPECTED_ROLE;

        if (!current.roleName().equalsIgnoreCase(expected)) {
            throw new RoleMismatchException(current.roleName(), expected);
        }

        return Response.ok(UserRestMapper.toResponse(current.user(), current.roleName())).build();
    }

    @POST
    @Path("/register")
    @Operation(summary = "Solicitar acceso — envía notificación al admin para aprobación")
    @APIResponse(responseCode = "202", description = "Solicitud recibida, pendiente de aprobación")
    @APIResponse(responseCode = "400", description = "Datos inválidos")
    @APIResponse(responseCode = "409", description = "El correo ya está registrado")
    public Response register(@Valid RegisterRequest request) {
        SolicitudAcceso solicitud = crearSolicitud.execute(
                request.nombre, request.correo, request.rol);

        String aprobarUrl = backendUrl + "/api/solicitudes/" + solicitud.getToken() + "/aprobar";
        String rechazarUrl = backendUrl + "/api/solicitudes/" + solicitud.getToken() + "/rechazar";

        mailer.send(Mail.withHtml(adminEmail,
                "Nueva solicitud de acceso — MedSync",
                buildEmailHtml(solicitud, aprobarUrl, rechazarUrl)));

        mailer.send(Mail.withHtml(solicitud.getCorreo(),
                "Solicitud de acceso recibida — MedSync",
                buildConfirmacionHtml(solicitud.getNombre())));

        return Response.accepted().build();
    }

    private static String buildConfirmacionHtml(String nombre) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><style>
                  body { font-family: sans-serif; background: #f8fafc; margin: 0; padding: 24px; }
                  .card { background: white; border-radius: 12px; padding: 32px; max-width: 480px;
                          margin: 0 auto; box-shadow: 0 2px 12px rgba(0,0,0,.08); }
                  h2 { color: #1e293b; margin-top: 0; }
                  p { color: #475569; line-height: 1.6; }
                  .footer { font-size: 12px; color: #94a3b8; margin-top: 24px; }
                </style></head>
                <body>
                  <div class="card">
                    <h2>📋 Solicitud recibida</h2>
                    <p>Hola <strong>%s</strong>, hemos recibido tu solicitud de acceso a MedSync.</p>
                    <p>Un administrador revisará tu información y recibirás una respuesta por este mismo correo.</p>
                    <p class="footer">Este es un correo automático. Por favor no respondas a este mensaje.</p>
                  </div>
                </body>
                </html>
                """.formatted(nombre);
    }

    private static String buildEmailHtml(SolicitudAcceso s, String aprobarUrl, String rechazarUrl) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><style>
                  body { font-family: sans-serif; background: #f8fafc; margin: 0; padding: 24px; }
                  .card { background: white; border-radius: 12px; padding: 32px; max-width: 480px;
                          margin: 0 auto; box-shadow: 0 2px 12px rgba(0,0,0,.08); }
                  h2 { color: #1e293b; margin-top: 0; }
                  .info { background: #f1f5f9; border-radius: 8px; padding: 16px; margin: 16px 0; }
                  .info p { margin: 4px 0; color: #475569; font-size: 14px; }
                  .info strong { color: #1e293b; }
                  .btn { display: inline-block; padding: 12px 24px; border-radius: 8px; text-decoration: none;
                         font-weight: 600; font-size: 14px; margin-right: 12px; }
                  .aprobar { background: #22c55e; color: white; }
                  .rechazar { background: #ef4444; color: white; }
                </style></head>
                <body>
                  <div class="card">
                    <h2>Nueva solicitud de acceso</h2>
                    <p>Un usuario ha solicitado acceso a MedSync.</p>
                    <div class="info">
                      <p><strong>Nombre:</strong> %s</p>
                      <p><strong>Correo:</strong> %s</p>
                      <p><strong>Rol:</strong> %s</p>
                    </div>
                    <a href="%s" class="btn aprobar">✅ Aprobar</a>
                    <a href="%s" class="btn rechazar">❌ Rechazar</a>
                  </div>
                </body>
                </html>
                """.formatted(s.getNombre(), s.getCorreo(), s.getRol(), aprobarUrl, rechazarUrl);
    }
}
