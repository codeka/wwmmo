package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import au.com.codeka.warworlds.model.protobuf.Messages;

public class ChatMessage {
    private String mMessage;
    private String mEmpireKey;
    private DateTime mDatePosted;

    public ChatMessage() {
    }
    public ChatMessage(String message) {
        mMessage = message;
    }

    public String getMessage() {
        return mMessage;
    }
    public void setMessage(String msg) {
        mMessage = msg;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }
    public DateTime getDatePosted() {
        return mDatePosted;
    }

    public static ChatMessage fromProtocolBuffer(Messages.ChatMessage pb) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.mMessage = pb.getMessage();
        chatMessage.mEmpireKey = pb.getEmpireKey();
        chatMessage.mDatePosted = new DateTime(pb.getDatePosted() * 1000, DateTimeZone.UTC);
        return chatMessage;
    }
}
