package au.com.codeka.warworlds.server.world.chat;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.ChatRoom;
import au.com.codeka.warworlds.server.store.DataStore;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * All the details we need about room, all the participants and so on.
 */
public class Room {
  private final ChatRoom room;
  private final Object lock = new Object();

  /** The current history, most recent message last. */
  private final List<ChatMessage> history;

  /**
   * The time that we have history loaded back until. We only load history as it's requested, and
   * not further back than needed. Mostly we don't need to keep history from much before when we're
   * created.
   */
  private long historyStartTime;

  public Room(ChatRoom room) {
    this.room = checkNotNull(room);
    this.historyStartTime = System.currentTimeMillis();
    this.history = new ArrayList<>();
  }

  public ChatRoom getChatRoom() {
    return room;
  }

  public void send(ChatMessage msg) {
    // TODO: make sure we're inserting in the right place
    history.add(msg);

    DataStore.i.chat().send(room, msg);
  }

  /**
   * Get the {@link ChatMessage}s send between startTime and endTime. ordered most recent message
   * last.
   */
  public List<ChatMessage> getMessages(long startTime, long endTime) {
    synchronized (lock) {
      if (historyStartTime > startTime) {
        List<ChatMessage> messages =
            DataStore.i.chat().getMessages(room.id, startTime, historyStartTime);
        history.addAll(0, messages);
        historyStartTime = startTime;
      }

      ArrayList<ChatMessage> messages = new ArrayList<>();
      // TODO: this is O(N^2) in the number of messages, we could make this O(N) by working out
      // how many messages there are in total, then populating an array with that many. We could
      // also make it O(N) by not trying to populate the list backwards, and maybe using a binary
      // search to find the first message to start at.
      for (int i = history.size() - 1; i >= 0; i--) {
        ChatMessage msg = history.get(i);
        if (msg.date_posted > startTime && msg.date_posted <= endTime) {
          messages.add(0, msg);
        } else if (msg.date_posted < startTime) {
          break;
        }
      }
      return messages;
    }
  }
}
