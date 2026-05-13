package itesm.medsync.interfaces.rest.vocabulary;

import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.user.model.KnownRoles;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.domain.vocabulary.model.Vocabulary;
import itesm.medsync.domain.vocabulary.usecase.GetVocabularyByEspecialidadUseCase;
import itesm.medsync.interfaces.rest.common.ErrorResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/api/especialidades/{id}/vocabulary")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "Operaciones administrativas")
public class VocabularyResource {

    private final GetVocabularyByEspecialidadUseCase getVocabulary;
    private final AuthenticatedUserContext userContext;

    @Inject
    public VocabularyResource(GetVocabularyByEspecialidadUseCase getVocabulary,
                              AuthenticatedUserContext userContext) {
        this.getVocabulary = getVocabulary;
        this.userContext = userContext;
    }

    @GET
    @Operation(summary = "Obtener el vocabulario controlado de una especialidad (COO)")
    @APIResponse(responseCode = "200")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "Solo el COO puede consultar el vocabulario")
    @APIResponse(responseCode = "404", description = "Especialidad no encontrada")
    public Response get(@PathParam("id") UUID id) {
        UserWithRole caller = userContext.getCurrentUser();
        if (caller == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (!KnownRoles.COO.equalsIgnoreCase(caller.roleName())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse(403, "Forbidden",
                            "Solo el COO puede consultar el vocabulario"))
                    .build();
        }
        Vocabulary v = getVocabulary.execute(id);
        return Response.ok(VocabularyRestMapper.toResponse(v)).build();
    }
}
