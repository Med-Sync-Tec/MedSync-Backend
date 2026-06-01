package itesm.medsync.infrastructure.ai;

import itesm.medsync.domain.articleaianalysis.exception.AiConfigurationException;
import itesm.medsync.domain.chat.exception.ChatException;
import itesm.medsync.domain.soapdictation.repository.WhisperGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Groq Whisper adapter for the {@link WhisperGateway} port.
 *
 * Sends audio as multipart/form-data to Groq's audio transcription endpoint.
 * Java's HttpClient has no native multipart support, so the boundary and parts
 * are assembled manually. The response is plain text (response_format=text).
 */
@ApplicationScoped
public class GroqWhisperGateway implements WhisperGateway {

    private static final Logger LOG = Logger.getLogger(GroqWhisperGateway.class);
    private static final String MODEL = "whisper-large-v3";
    private static final String LANGUAGE = "es";

    private final GroqAiAnalysisGatewayConfig config;
    private final HttpClient httpClient;

    @Inject
    public GroqWhisperGateway(GroqAiAnalysisGatewayConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.timeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public String transcribe(byte[] audioBytes, String mimeType) {
        if (!config.isConfigured()) {
            throw new AiConfigurationException(
                    "GROQ_API_KEY is not set — cannot use Whisper transcription");
        }

        String boundary = "boundary_" + System.currentTimeMillis();
        byte[] body = buildMultipartBody(boundary, audioBytes, mimeType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/audio/transcriptions"))
                .timeout(config.timeout())
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException ex) {
            LOG.errorf(ex, "Groq Whisper call timed out");
            throw new ChatException("Groq Whisper call exceeded timeout", ex);
        } catch (IOException ex) {
            LOG.errorf(ex, "Groq Whisper call I/O error: %s", ex.getClass().getName());
            throw new ChatException("Groq Whisper call failed [" + ex.getClass().getSimpleName() + "]: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ChatException("Groq Whisper call interrupted", ex);
        }

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            String hint = status == 401 ? " (invalid GROQ_API_KEY?)" : "";
            LOG.errorf("Groq Whisper returned HTTP %d", status);
            throw new ChatException("Groq Whisper returned HTTP " + status + hint);
        }

        return response.body();
    }

    private byte[] buildMultipartBody(String boundary, byte[] audioBytes, String mimeType) {
        String crlf = "\r\n";
        String dashes = "--";

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Part: file
            write(out, dashes + boundary + crlf);
            write(out, "Content-Disposition: form-data; name=\"file\"; filename=\"audio.webm\"" + crlf);
            write(out, "Content-Type: " + mimeType + crlf + crlf);
            out.write(audioBytes);
            write(out, crlf);

            // Part: model
            write(out, dashes + boundary + crlf);
            write(out, "Content-Disposition: form-data; name=\"model\"" + crlf + crlf);
            write(out, MODEL + crlf);

            // Part: language
            write(out, dashes + boundary + crlf);
            write(out, "Content-Disposition: form-data; name=\"language\"" + crlf + crlf);
            write(out, LANGUAGE + crlf);

            // Part: response_format
            write(out, dashes + boundary + crlf);
            write(out, "Content-Disposition: form-data; name=\"response_format\"" + crlf + crlf);
            write(out, "text" + crlf);

            // End boundary
            write(out, dashes + boundary + dashes + crlf);

            return out.toByteArray();
        } catch (IOException ex) {
            throw new ChatException("Failed to build multipart body", ex);
        }
    }

    private static void write(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.UTF_8));
    }
}
