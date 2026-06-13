package itesm.medsync.interfaces.rest.soapdictation;

import itesm.medsync.domain.soapdictation.model.SOAPDictationResult;
import itesm.medsync.domain.soapdictation.usecase.TranscribeAndExtractSOAPUseCase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SOAPDictationResourceTest {

    @Mock
    TranscribeAndExtractSOAPUseCase useCase;

    @InjectMocks
    SOAPDictationResource resource;

    private static final SOAPDictationResult FULL_RESULT = new SOAPDictationResult(
            "Dolor abdominal", "Fiebre y dolor", "Abdomen rígido",
            "Apendicitis probable", "Apendicitis aguda", "Derivar a cirugía", "Ibuprofeno 400mg"
    );

    @Test
    @DisplayName("Valid audio upload → 200 with all SOAP fields")
    void validUpload() throws Exception {
        when(useCase.transcribeAndExtract(any(byte[].class), anyString()))
                .thenReturn(FULL_RESULT);

        InputStream audioStream = new ByteArrayInputStream("fake-audio".getBytes());
        Response response = resource.dictate(audioStream, "audio/webm");

        assertEquals(200, response.getStatus());
        SOAPDictationResponse body = (SOAPDictationResponse) response.getEntity();
        assertEquals("Dolor abdominal", body.motivoConsulta());
        assertEquals("Apendicitis aguda", body.diagnostico());
    }

    @Test
    @DisplayName("Use case is called with audio bytes and mime type")
    void useCaseCalled() throws Exception {
        when(useCase.transcribeAndExtract(any(byte[].class), anyString()))
                .thenReturn(FULL_RESULT);

        resource.dictate(new ByteArrayInputStream("audio".getBytes()), "audio/webm");

        verify(useCase).transcribeAndExtract(any(byte[].class), anyString());
    }

    @Test
    @DisplayName("Partial result → null fields are present in response")
    void partialResult() throws Exception {
        SOAPDictationResult partial = new SOAPDictationResult(
                "Dolor de cabeza", "Cefalea", null, null, null, null, null);
        when(useCase.transcribeAndExtract(any(byte[].class), anyString()))
                .thenReturn(partial);

        Response response = resource.dictate(
                new ByteArrayInputStream("audio".getBytes()), "audio/webm");

        assertEquals(200, response.getStatus());
        SOAPDictationResponse body = (SOAPDictationResponse) response.getEntity();
        assertEquals("Dolor de cabeza", body.motivoConsulta());
        assertNull(body.diagnostico());
    }
}
