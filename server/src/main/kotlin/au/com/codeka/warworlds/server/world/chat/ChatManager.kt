package au.com.codeka.warworlds.server.world.chat

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.ChatMessage
import au.com.codeka.warworlds.common.proto.ChatRoom
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.world.chat.Participant.OnlineCallback
import com.google.common.base.Preconditions
import java.util.*

/**
 * Manages chat rooms and stuff.
 */
class ChatManager {
  private val participants: MutableMap<Long, Participant> = HashMap()
  private val rooms: Map<Long, Room> = HashMap()
  private val globalRoom: Room

  /** "Send" the given message to the given room.  */
  fun send(roomId: Long?, msg: ChatMessage) {
    var msg = msg
    val room = getRoom(roomId)
    if (room == null) {
      log.error("No room with id %d", roomId)
      return
    }

    // TODO: validate the action, message_en, etc etc.
    msg = msg.newBuilder()
        .date_posted(System.currentTimeMillis())
        .id(DataStore.Companion.i.seq().nextIdentifier())
        .build()
    room.send(msg)
  }

  /** Get the history of all messages in the given room, between the given start and end time.  */
  fun getMessages(roomId: Long?, startTime: Long, endTime: Long): List<ChatMessage> {
    val room = getRoom(roomId)
    return room!!.getMessages(startTime, endTime)
  }

  /**
   * Called when a player connects. We'll start sending them messages and stuff.
   *
   * @param empireId The ID of the empire connecting.
   * @param lastChatTime The time this player last received a chat message. We'll straight away
   * send them any new ones since then.
   * @param callback A callback to call to send chat messages across.
   */
  fun connectPlayer(empireId: Long, lastChatTime: Long, callback: OnlineCallback) {
    var participant: Participant?
    synchronized(participants) {
      participant = participants[empireId]
      if (participant == null) {
        participant = Participant(empireId)
        participants[empireId] = participant!!
      }
    }

    // Players are only in the global room when they first connect.
    globalRoom.addParticipant(participant)

    // TODO: rooms
    val msgs = globalRoom.getMessages(lastChatTime, System.currentTimeMillis())
    callback.onChatMessage(msgs)
    participant!!.setOnlineCallback(callback)
  }

  /** Called when a player disconnects.  */
  fun disconnectPlayer(empireId: Long) {
    var participant: Participant?
    synchronized(participants) {
      participant = participants[empireId]
      if (participant == null) {
        return
      }
    }
    globalRoom.removeParticipant(participant)
    participant!!.setOnlineCallback(null)
  }

  /**
   * Gets the [ChatRoom] with the given identifier.
   *
   * @param id the identifier of the room, or null for the global room.
   * @return the room, or null if no room with that ID exists.
   */
  private fun getRoom(id: Long?): Room? {
    if (id == null) {
      return globalRoom
    }
    synchronized(rooms) {
      val room = rooms[id]
      if (room == null) {
        //TODO room = new WatchableObject<>(DataStore.i.chat().getRoom(id));
      }
      return room
    }
  }

  companion object {
    private val log = Log("ChatManager")
    val i = ChatManager()
  }

  init {
    globalRoom = Room(ChatRoom.Builder().name("Global").build())
  }
}