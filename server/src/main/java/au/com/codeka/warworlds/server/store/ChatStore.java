package au.com.codeka.warworlds.server.store;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.ChatRoom;
import au.com.codeka.warworlds.server.store.base.BaseStore;
import au.com.codeka.warworlds.server.store.base.QueryResult;
import au.com.codeka.warworlds.server.store.base.StoreReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A store that holds all the chat messages.
 */
public class ChatStore extends BaseStore {
  private static final Log log = new Log("ChatStore");

  ChatStore(String fileName) {
    super(fileName);
  }

  /**
   * "Send" the given message to the given room. Actually, just add the message to the room's
   * history.
   */
  public void send(ChatRoom room, ChatMessage msg) {
    try {
      newWriter()
          .stmt("INSERT INTO messages (id, room_id, date, msg) VALUES (?, ?, ?, ?)")
          .param(0, msg.id)
          .param(1, room.id)
          .param(2, msg.date_posted)
          .param(3, msg.encode())
          .execute();
    } catch (StoreException e) {
      log.error("Unexpected.", e);
    }
  }

  /**
   * Gets all of the messages in the given room between the given start time and end time, ordered
   * most recent message last.
   */
  public List<ChatMessage> getMessages(Long roomId, long startTime, long endTime) {
    StoreReader reader = newReader();
    if (roomId == null) {
      reader.stmt("SELECT msg FROM messages WHERE room_id IS NULL AND date > ? AND date <= ?")
          .param(0, startTime)
          .param(1, endTime);
    } else {
      reader.stmt("SELECT msg FROM messages WHERE room_id = ? AND date > ? AND date <= ?")
          .param(0, roomId)
          .param(1, startTime)
          .param(2, endTime);
    }

    ArrayList<ChatMessage> msgs = new ArrayList<>();
    try (QueryResult res = reader.query()) {
      while (res.next()) {
        msgs.add(ChatMessage.ADAPTER.decode(res.getBytes(0)));
      }
    } catch (Exception e) {
      log.error("Unexpected.", e);
    }
    return msgs;
  }

  @Override
  protected int onOpen(int diskVersion) throws StoreException {
    if (diskVersion == 0) {
      newWriter()
          .stmt("CREATE TABLE rooms (id INTEGER PRIMARY KEY, room BLOB)")
          .execute();
      newWriter()
          .stmt("CREATE TABLE participants (empire_id INTEGER, room_id INTEGER, participant BLOB)")
          .execute();
      newWriter()
          .stmt("CREATE INDEX IX_participants_room ON participants (room_id, empire_id)")
          .execute();
      newWriter()
          .stmt("CREATE INDEX IX_participants_empire ON participants (empire_id, room_id)")
          .execute();

      newWriter()
          .stmt("CREATE TABLE messages (id INTEGER PRIMARY KEY, room_id INTEGER, date INTEGER, msg BLOB)")
          .execute();
      newWriter()
          .stmt("CREATE INDEX IX_messages_room ON messages (room_id)");

      diskVersion++;
    }

    return diskVersion;
  }
}
