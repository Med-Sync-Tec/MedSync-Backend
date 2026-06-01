package itesm.medsync.application.chat;

import itesm.medsync.domain.chat.model.ChatRequest;
import itesm.medsync.domain.chat.model.ChatResponse;
import itesm.medsync.domain.chat.repository.ChatGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    ChatGateway gateway;

    @InjectMocks
    ChatService service;

    @Test
    @DisplayName("Normal response → isCritical=false, text returned as-is")
    void normalResponse() {
        when(gateway.complete(anyString(), eq("What is ibuprofen?"))).thenReturn("Ibuprofen is an NSAID.");

        ChatResponse result = service.chat(new ChatRequest("What is ibuprofen?"));

        assertFalse(result.isCritical());
        assertEquals("Ibuprofen is an NSAID.", result.response());
    }

    @Test
    @DisplayName("Response starting with CRITICAL: → isCritical=true, canned message returned")
    void criticalPrefix() {
        when(gateway.complete(anyString(), anyString()))
                .thenReturn("CRITICAL: Para situaciones de emergencia consulta el protocolo.");

        ChatResponse result = service.chat(new ChatRequest("¿Cómo trato un paro cardíaco?"));

        assertTrue(result.isCritical());
        assertEquals(ChatService.CANNED_CRITICAL, result.response());
    }

    @Test
    @DisplayName("CRITICAL: in the middle of the text → isCritical=false (only prefix counts)")
    void criticalNotAtStart() {
        String text = "El medicamento es útil. CRITICAL: no es emergencia.";
        when(gateway.complete(anyString(), anyString())).thenReturn(text);

        ChatResponse result = service.chat(new ChatRequest("¿Para qué sirve el losartán?"));

        assertFalse(result.isCritical());
        assertEquals(text, result.response());
    }

    @Test
    @DisplayName("Response with leading/trailing whitespace → response is stripped")
    void stripsWhitespace() {
        when(gateway.complete(anyString(), anyString())).thenReturn("  Respuesta limpia.  ");

        ChatResponse result = service.chat(new ChatRequest("pregunta"));

        assertEquals("Respuesta limpia.", result.response());
    }

    @Test
    @DisplayName("System prompt is passed to gateway")
    void systemPromptPassed() {
        when(gateway.complete(anyString(), anyString())).thenReturn("ok");

        service.chat(new ChatRequest("hola"));

        verify(gateway).complete(eq(ChatService.SYSTEM_PROMPT), eq("hola"));
    }
}
