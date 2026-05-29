package itesm.medsync.domain.soapdictation.repository;

public interface WhisperGateway {
    /** Transcribes audio bytes to text. */
    String transcribe(byte[] audioBytes, String mimeType);
}
