package itesm.medsync.interfaces.rest.soapdictation;

import itesm.medsync.domain.soapdictation.model.SOAPDictationResult;
import itesm.medsync.domain.soapdictation.usecase.TranscribeAndExtractSOAPUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Path("/api/soap")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "SOAP Dictation", description = "Transcripción y extracción de notas SOAP por voz")
public class SOAPDictationResource {

    private final TranscribeAndExtractSOAPUseCase useCase;

    @Inject
    public SOAPDictationResource(TranscribeAndExtractSOAPUseCase useCase) {
        this.useCase = useCase;
    }

    @POST
    @Path("/dictation")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Transcribe y extrae campos SOAP de un audio dictado")
    @APIResponse(responseCode = "200", description = "Campos SOAP extraídos")
    @APIResponse(responseCode = "400", description = "Sin campo audio o archivo inválido")
    @APIResponse(responseCode = "401", description = "No autenticado")
    public Response dictate(@RestForm("audio") FileUpload audio) throws IOException {
        byte[] bytes = Files.readAllBytes(audio.uploadedFile());
        String mimeType = audio.contentType() != null ? audio.contentType() : "audio/webm";
        return buildResponse(useCase.transcribeAndExtract(bytes, mimeType));
    }

    // Package-visible overload used in unit tests (avoids FileUpload dependency)
    Response dictate(InputStream audioStream, String mimeType) throws IOException {
        byte[] bytes = audioStream.readAllBytes();
        return buildResponse(useCase.transcribeAndExtract(bytes, mimeType));
    }

    private Response buildResponse(SOAPDictationResult result) {
        return Response.ok(new SOAPDictationResponse(
                result.motivoConsulta(),
                result.subjetivo(),
                result.objetivo(),
                result.evaluacion(),
                result.diagnostico(),
                result.plan(),
                result.prescripcion()
        )).build();
    }
}
