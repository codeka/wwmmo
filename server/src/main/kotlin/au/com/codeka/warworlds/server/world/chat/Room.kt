package au.com.codeka.warworlds.server.world.chat

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.ChatMessage
import au.com.codeka.warworlds.common.proto.ChatRoom
import au.com.codeka.warworlds.server.store.DataStore
import com.google.common.base.Preconditions
import java.util.*

/**
 * All the details we need about room, all the participants and so on.
 */
class Room(room: ChatRoom) {
  val chatRoom: ChatRoom
  private val lock = Any()

  /** The current history, most recent message last.  */
  private val history: MutableList<ChatMessage>

  /** The [Participant]s in this room.  */
  private val participants: MutableList<Participant?>

  /**
   * The time that we have history loaded back until. We only load history as it's requested, and
   * not further back than needed. Mostly we don't need to keep history from much before when we're
   * created.
   */
  private var historyStartTime: Long

  fun send(msg: ChatMessage) {
    // TODO: make sure we're inserting in the right place
    synchronized(lock) { history.add(msg) }
    synchronized(participants) {
      for (participant in participants) {
        participant!!.onMessage(msg)
      }
    }
    DataStore.Companion.i.chat().send(chatRoom, msg)
  }

  fun addParticipant(participant: Participant?) {
    synchronized(participants) { participants.add(participant) }
  }

  fun removeParticipant(participant: Participant?) {
    synchronized(participants) { participants.remove(participant) }
  }

  /**
   * Get the [ChatMessage]s send between startTime and endTime. ordered most recent message
   * last.
   */
  fun getMessages(startTime: Long, endTime: Long): List<ChatMessage> {
    synchronized(lock) {
      if (historyStartTime > startTime) {
        val messages: List<ChatMessage> = DataStore.Companion.i.chat().getMessages(chatRoom.id, startTime, historyStartTime)
        history.addAll(0, messages)
        historyStartTime = startTime
      }
      val messages = ArrayList<ChatMessage>()
      // TODO: this is O(N^2) in the number of messages, we could make this O(N) by working out
      // how many messages there are in total, then populating an array with that many. We could
      // also make it O(N) by not trying to populate the list backwards, and maybe using a binary
      // search to find the first message to start at.
      for (i in history.indices.reversed()) {
        val msg = history[i]
        if (msg.date_posted > startTime && msg.date_posted <= endTime) {
          messages.add(0, msg)
        } else if (msg.date_posted < startTime) {
          break
        }
      }
      return messages
    }
  }

  companion object {
    private val log = Log("Room")
  }

  init {
    chatRoom = Preconditions.checkNotNull(room)
    historyStartTime = System.currentTimeMillis()
    history = ArrayList()
    participants = ArrayList()
  }
}