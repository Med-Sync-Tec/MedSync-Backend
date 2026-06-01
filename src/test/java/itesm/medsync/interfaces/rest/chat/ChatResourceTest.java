package itesm.medsync.interfaces.rest.chat;

import itesm.medsync.domain.chat.model.ChatRequest;
import itesm.medsync.domain.chat.model.ChatResponse;
import itesm.medsync.domain.chat.usecase.ChatUseCase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatResourceTest {

    @Mock
    ChatUseCase chatUseCase;

    @InjectMocks
    ChatResource resource;

    @Test
    @DisplayName("Valid message → 200 with response and isCritical")
    void validMessage() {
        when(chatUseCase.chat(any(ChatRequest.class)))
                .thenReturn(new ChatResponse("El ibuprofeno es un antiinflamatorio.", false));

        itesm.medsync.interfaces.rest.chat.ChatRequest dto = new itesm.medsync.interfaces.rest.chat.ChatRequest();
        dto.message = "¿Qué es el ibuprofeno?";

        Response response = resource.chat(dto);

        assertEquals(200, response.getStatus());
        ChatResponseDto body = (ChatResponseDto) response.getEntity();
        assertEquals("El ibuprofeno es un antiinflamatorio.", body.response);
        assertFalse(body.isCritical);
    }

    @Test
    @DisplayName("Critical question → 200 with isCritical=true and canned message")
    void criticalQuestion() {
        when(chatUseCase.chat(any(ChatRequest.class)))
                .thenReturn(new ChatResponse("Consulta al personal de guardia.", true));

        itesm.medsync.interfaces.rest.chat.ChatRequest dto = new itesm.medsync.interfaces.rest.chat.ChatRequest();
        dto.message = "¿Cómo atiendo un paro cardíaco?";

        Response response = resource.chat(dto);

        assertEquals(200, response.getStatus());
        ChatResponseDto body = (ChatResponseDto) response.getEntity();
        assertTrue(body.isCritical);
    }

    @Test
    @DisplayName("Use case is called with the message from the DTO")
    void messagePassedToUseCase() {
        when(chatUseCase.chat(any())).thenReturn(new ChatResponse("ok", false));

        itesm.medsync.interfaces.rest.chat.ChatRequest dto = new itesm.medsync.interfaces.rest.chat.ChatRequest();
        dto.message = "mi pregunta";

        resource.chat(dto);

        verify(chatUseCase).chat(new ChatRequest("mi pregunta"));
    }
}
