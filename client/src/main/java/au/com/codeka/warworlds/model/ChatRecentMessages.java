package au.com.codeka.warworlds.model;

import java.util.LinkedList;
import java.util.List;

/** Container for all recent (last 10) messages that we display in the MiniChatView. */
public class ChatRecentMessages {
  public static final int MAX_MESSAGES = 10;
  private final LinkedList<ChatMessage> messages = new LinkedList<>();

  public boolean addMessage(ChatMessage msg) {
    synchronized (messages) {
      // make sure we don't have this chat already...
      for (ChatMessage existing : messages) {
        if (existing.getID() == msg.getID()) {
          return false;
        }
      }

      while (messages.size() >= MAX_MESSAGES) {
        messages.removeFirst();
      }
    }

    messages.addLast(msg);
    return true;
  }

  public List<ChatMessage> getMessages() {
    return messages;
  }

  public void clear() {
    messages.clear();
  }
}
