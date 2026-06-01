package itesm.medsync.interfaces.rest.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRequest {
    @NotBlank
    @Size(max = 1000)
    public String message;
}
