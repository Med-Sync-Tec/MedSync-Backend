package itesm.medsync.application.soapdictation;

import itesm.medsync.domain.chat.repository.ChatGateway;
import itesm.medsync.domain.soapdictation.model.SOAPDictationResult;
import itesm.medsync.domain.soapdictation.repository.WhisperGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscribeAndExtractSOAPServiceTest {

    @Mock
    WhisperGateway whisperGateway;

    @Mock
    ChatGateway chatGateway;

    @InjectMocks
    TranscribeAndExtractSOAPService service;

    @Test
    @DisplayName("Full transcript → all 7 SOAP fields populated")
    void fullTranscript() {
        when(whisperGateway.transcribe(any(byte[].class), anyString()))
                .thenReturn("Paciente con dolor abdominal, fiebre, apendicitis, ibuprofeno.");
        when(chatGateway.completeJson(anyString(), anyString())).thenReturn("""
                {
                  "motivoConsulta": "Dolor abdominal",
                  "subjetivo": "Fiebre y dolor",
                  "objetivo": "Abdomen rígido",
                  "evaluacion": "Apendicitis probable",
                  "diagnostico": "Apendicitis aguda",
                  "plan": "Derivar a cirugía",
                  "prescripcion": "Ibuprofeno 400mg"
                }
                """);

        SOAPDictationResult result = service.transcribeAndExtract(
                "audio".getBytes(), "audio/webm");

        assertEquals("Dolor abdominal", result.motivoConsulta());
        assertEquals("Fiebre y dolor", result.subjetivo());
        assertEquals("Abdomen rígido", result.objetivo());
        assertEquals("Apendicitis probable", result.evaluacion());
        assertEquals("Apendicitis aguda", result.diagnostico());
        assertEquals("Derivar a cirugía", result.plan());
        assertEquals("Ibuprofeno 400mg", result.prescripcion());
    }

    @Test
    @DisplayName("Partial transcript → missing fields are null")
    void partialTranscript() {
        when(whisperGateway.transcribe(any(byte[].class), anyString()))
                .thenReturn("Paciente con dolor de cabeza.");
        when(chatGateway.completeJson(anyString(), anyString())).thenReturn("""
                {
                  "motivoConsulta": "Dolor de cabeza",
                  "subjetivo": "Cefalea intensa",
                  "objetivo": null,
                  "evaluacion": null,
                  "diagnostico": null,
                  "plan": null,
                  "prescripcion": null
                }
                """);

        SOAPDictationResult result = service.transcribeAndExtract(
                "audio".getBytes(), "audio/webm");

        assertEquals("Dolor de cabeza", result.motivoConsulta());
        assertEquals("Cefalea intensa", result.subjetivo());
        assertNull(result.objetivo());
        assertNull(result.diagnostico());
    }

    @Test
    @DisplayName("Invalid JSON from LLaMA → exception propagated")
    void invalidJson() {
        when(whisperGateway.transcribe(any(byte[].class), anyString()))
                .thenReturn("some text");
        when(chatGateway.completeJson(anyString(), anyString()))
                .thenReturn("not valid json at all {{{");

        assertThrows(Exception.class,
                () -> service.transcribeAndExtract("audio".getBytes(), "audio/webm"));
    }

    @Test
    @DisplayName("Whisper failure → exception propagated")
    void whisperFailure() {
        when(whisperGateway.transcribe(any(byte[].class), anyString()))
                .thenThrow(new RuntimeException("Groq Whisper failed"));

        assertThrows(RuntimeException.class,
                () -> service.transcribeAndExtract("audio".getBytes(), "audio/webm"));
    }
}
