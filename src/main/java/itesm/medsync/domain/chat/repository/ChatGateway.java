package itesm.medsync.domain.chat.repository;

public interface ChatGateway {
    /** Sends a single user message and returns the assistant reply in free text. */
    String complete(String systemPrompt, String userMessage);

    /** Same as complete() but forces JSON mode — response is guaranteed to be a JSON object. */
    String completeJson(String systemPrompt, String userMessage);
}
