package itesm.medsync.domain.chat.usecase;

import itesm.medsync.domain.chat.model.ChatRequest;
import itesm.medsync.domain.chat.model.ChatResponse;

public interface ChatUseCase {
    ChatResponse chat(ChatRequest request);
}
