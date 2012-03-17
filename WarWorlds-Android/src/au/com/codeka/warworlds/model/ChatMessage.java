package au.com.codeka.warworlds.model;

/**
 * Represents a single chat message.
 */
public class ChatMessage {
    private String mMessage;

    public ChatMessage() {
    }
    public ChatMessage(String message) {
        mMessage = message;
    }

    public String getMessage() {
        return mMessage;
    }
}
