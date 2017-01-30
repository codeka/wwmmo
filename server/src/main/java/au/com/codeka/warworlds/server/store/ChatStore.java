package au.com.codeka.warworlds.server.store;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.ChatRoom;
import au.com.codeka.warworlds.server.store.base.BaseStore;

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
