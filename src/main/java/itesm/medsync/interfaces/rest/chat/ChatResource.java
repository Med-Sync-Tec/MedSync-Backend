package itesm.medsync.interfaces.rest.chat;

import itesm.medsync.domain.chat.model.ChatRequest;
import itesm.medsync.domain.chat.model.ChatResponse;
import itesm.medsync.domain.chat.usecase.ChatUseCase;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Chat", description = "MediBot — asistente de preguntas médicas no críticas")
public class ChatResource {

    private final ChatUseCase chatUseCase;

    @Inject
    public ChatResource(ChatUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }

    @POST
    @Operation(summary = "Envía un mensaje al asistente MediBot")
    @APIResponse(responseCode = "200", description = "Respuesta del asistente")
    @APIResponse(responseCode = "400", description = "Mensaje vacío o demasiado largo")
    @APIResponse(responseCode = "401", description = "No autenticado")
    public Response chat(@Valid itesm.medsync.interfaces.rest.chat.ChatRequest request) {
        ChatResponse result = chatUseCase.chat(new ChatRequest(request.message));
        return Response.ok(new ChatResponseDto(result.response(), result.isCritical())).build();
    }
}
