package au.com.codeka.warworlds.model;

import au.com.codeka.warworlds.model.protobuf.Messages;

/**
 * Represents a single chat message.
 */
public class ChatMessage {
    private String mMessage;
    private String mEmpireKey;

    public ChatMessage() {
    }
    public ChatMessage(String message) {
        mMessage = message;
    }

    public String getMessage() {
        return mMessage;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }

    public static ChatMessage fromProtocolBuffer(Messages.ChatMessage pb) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.mMessage = pb.getMessage();
        chatMessage.mEmpireKey = pb.getEmpireKey();
        return chatMessage;
    }
}
