package itesm.medsync.domain.chat.repository;

public interface ChatGateway {
    /** Sends a single user message with a system prompt and returns the assistant reply. */
    String complete(String systemPrompt, String userMessage);
}
