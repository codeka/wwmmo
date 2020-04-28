package au.com.codeka.warworlds.server.store

import au.com.codeka.warworlds.common.proto.ChatMessage
import au.com.codeka.warworlds.common.proto.ChatRoom
import au.com.codeka.warworlds.server.store.base.BaseStore
import java.util.*

/**
 * A store that holds all the chat messages.
 */
class ChatStore internal constructor(fileName: String) : BaseStore(fileName) {
  /**
   * "Send" the given message to the given room. Actually, just add the message to the room's
   * history.
   */
  fun send(room: ChatRoom, msg: ChatMessage) {
    newWriter()
        .stmt("INSERT INTO messages (id, room_id, date, msg) VALUES (?, ?, ?, ?)")
        .param(0, msg.id)
        .param(1, room.id)
        .param(2, msg.date_posted)
        .param(3, msg.encode())
        .execute()
  }

  /**
   * Gets all of the messages in the given room between the given start time and end time, ordered
   * most recent message last.
   */
  fun getMessages(roomId: Long?, startTime: Long, endTime: Long): List<ChatMessage> {
    val reader = newReader()
    if (roomId == null) {
      reader.stmt("SELECT msg FROM messages WHERE room_id IS NULL AND date > ? AND date <= ?")
          .param(0, startTime)
          .param(1, endTime)
    } else {
      reader.stmt("SELECT msg FROM messages WHERE room_id = ? AND date > ? AND date <= ?")
          .param(0, roomId)
          .param(1, startTime)
          .param(2, endTime)
    }
    val msgs = ArrayList<ChatMessage>()
    reader.query().use { res ->
      while (res.next()) {
        msgs.add(ChatMessage.ADAPTER.decode(res.getBytes(0)))
      }
    }
    return msgs
  }

  override fun onOpen(diskVersion: Int): Int {
    var version = diskVersion
    if (version == 0) {
      newWriter()
          .stmt("CREATE TABLE rooms (id INTEGER PRIMARY KEY, room BLOB)")
          .execute()
      newWriter()
          .stmt("CREATE TABLE participants (empire_id INTEGER, room_id INTEGER, participant BLOB)")
          .execute()
      newWriter()
          .stmt("CREATE INDEX IX_participants_room ON participants (room_id, empire_id)")
          .execute()
      newWriter()
          .stmt("CREATE INDEX IX_participants_empire ON participants (empire_id, room_id)")
          .execute()
      newWriter()
          .stmt("CREATE TABLE messages (id INTEGER PRIMARY KEY, room_id INTEGER, date INTEGER, msg BLOB)")
          .execute()
      newWriter()
          .stmt("CREATE INDEX IX_messages_room ON messages (room_id)")
      version++
    }
    return version
  }
}