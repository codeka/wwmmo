package au.com.codeka.warworlds.model;

import java.util.LinkedList;
import java.util.List;

/** Container for all recent (last 10) messages that we display in the MiniChatView. */
public class ChatRecentMessages {
    public static final int MAX_MESSAGES = 10;
    private LinkedList<ChatMessage> mMessages = new LinkedList<ChatMessage>();

    public boolean addMessage(ChatMessage msg) {
        synchronized(mMessages) {
            // make sure we don't have this chat already...
            for (ChatMessage existing : mMessages) {
                if (existing.getID() == msg.getID()) {
                    return false;
                }
            }

            while (mMessages.size() >= MAX_MESSAGES) {
                mMessages.removeFirst();
            }
        }

        mMessages.addLast(msg);
        return true;
    }

    public List<ChatMessage> getMessages() {
        return mMessages;
    }
}
