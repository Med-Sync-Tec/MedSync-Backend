package itesm.medsync.interfaces.rest.solicitud;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import itesm.medsync.domain.solicitud.model.AprobarSolicitudResult;
import itesm.medsync.domain.solicitud.model.SolicitudAcceso;
import itesm.medsync.domain.solicitud.usecase.AprobarSolicitudUseCase;
import itesm.medsync.domain.solicitud.usecase.RechazarSolicitudUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/solicitudes")
@Tag(name = "Solicitudes", description = "Aprobación y rechazo de solicitudes de acceso")
public class SolicitudResource {

    private final AprobarSolicitudUseCase aprobar;
    private final RechazarSolicitudUseCase rechazar;
    private final Mailer mailer;

    @Inject
    public SolicitudResource(AprobarSolicitudUseCase aprobar,
                              RechazarSolicitudUseCase rechazar,
                              Mailer mailer) {
        this.aprobar = aprobar;
        this.rechazar = rechazar;
        this.mailer = mailer;
    }

    @GET
    @Path("/{token}/aprobar")
    @Produces(MediaType.TEXT_HTML)
    @Operation(summary = "Aprobar solicitud de acceso")
    public Response aprobar(@PathParam("token") String token) {
        AprobarSolicitudResult result = aprobar.execute(token);
        String nombre = result.user().user().getNombre();
        String correo = result.user().user().getCorreo();
        String rol = result.user().roleName();

        mailer.send(Mail.withHtml(
                correo,
                "Tu acceso a MedSync ha sido aprobado",
                buildAprobadoHtml(nombre, rol, result.passwordResetLink())));

        return Response.ok(htmlPage(
                "✅ Solicitud aprobada",
                "El usuario ha sido creado exitosamente. Se le envió un correo para establecer su contraseña."
        )).build();
    }

    @GET
    @Path("/{token}/rechazar")
    @Produces(MediaType.TEXT_HTML)
    @Operation(summary = "Rechazar solicitud de acceso")
    public Response rechazar(@PathParam("token") String token) {
        SolicitudAcceso solicitud = rechazar.execute(token);

        mailer.send(Mail.withHtml(
                solicitud.getCorreo(),
                "Tu solicitud de acceso a MedSync fue rechazada",
                buildRechazadoHtml(solicitud.getNombre())));

        return Response.ok(htmlPage(
                "❌ Solicitud rechazada",
                "La solicitud de acceso ha sido rechazada y se notificó al usuario."
        )).build();
    }

    private static String buildAprobadoHtml(String nombre, String rol, String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><style>
                  body { font-family: sans-serif; background: #f8fafc; margin: 0; padding: 24px; }
                  .card { background: white; border-radius: 12px; padding: 32px; max-width: 480px;
                          margin: 0 auto; box-shadow: 0 2px 12px rgba(0,0,0,.08); }
                  h2 { color: #1e293b; margin-top: 0; }
                  .badge { display: inline-block; background: #dcfce7; color: #166534;
                           border-radius: 6px; padding: 4px 12px; font-size: 13px; font-weight: 600; }
                  p { color: #475569; line-height: 1.6; }
                  .btn { display: inline-block; margin-top: 16px; padding: 12px 24px; background: #4f46e5;
                         color: #ffffff !important; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px; }
                  .btn:visited { color: #ffffff !important; }
                  .footer { font-size: 12px; color: #94a3b8; margin-top: 24px; }
                </style></head>
                <body>
                  <div class="card">
                    <h2>✅ ¡Tu solicitud fue aprobada!</h2>
                    <p>Hola <strong>%s</strong>, tu cuenta en MedSync ha sido creada con el rol <span class="badge">%s</span>.</p>
                    <p>Para activar tu cuenta, establece tu contraseña haciendo clic en el botón:</p>
                    <a href="%s" class="btn">Establecer contraseña</a>
                    <p class="footer">Este enlace es de un solo uso. Si no solicitaste acceso a MedSync, ignora este correo.</p>
                  </div>
                </body>
                </html>
                """.formatted(nombre, rol, resetLink);
    }

    private static String buildRechazadoHtml(String nombre) {
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
                    <h2>❌ Tu solicitud no fue aprobada</h2>
                    <p>Hola <strong>%s</strong>, lamentablemente tu solicitud de acceso a MedSync fue rechazada.</p>
                    <p>Si crees que esto es un error, contacta al administrador del sistema.</p>
                    <p class="footer">Este es un correo automático.</p>
                  </div>
                </body>
                </html>
                """.formatted(nombre);
    }

    private static String htmlPage(String titulo, String mensaje) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>MedSync</title>
                  <style>
                    body { font-family: sans-serif; display: flex; align-items: center; justify-content: center;
                           min-height: 100vh; margin: 0; background: #f8fafc; }
                    .card { background: white; border-radius: 16px; padding: 40px; text-align: center;
                            box-shadow: 0 4px 24px rgba(0,0,0,.08); max-width: 400px; }
                    h1 { font-size: 1.5rem; margin-bottom: 12px; color: #1e293b; }
                    p { color: #64748b; line-height: 1.6; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>%s</h1>
                    <p>%s</p>
                  </div>
                </body>
                </html>
                """.formatted(titulo, mensaje);
    }
}
