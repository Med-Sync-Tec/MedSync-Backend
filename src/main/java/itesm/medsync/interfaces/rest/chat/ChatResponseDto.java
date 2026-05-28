package itesm.medsync.interfaces.rest.chat;

public class ChatResponseDto {
    public String response;
    public boolean isCritical;

    public ChatResponseDto(String response, boolean isCritical) {
        this.response = response;
        this.isCritical = isCritical;
    }
}
