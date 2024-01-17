package au.com.codeka.warworlds.client.game.world

import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import au.com.codeka.warworlds.common.Time
import au.com.codeka.warworlds.common.proto.ChatMessage
import au.com.codeka.warworlds.common.proto.ChatMessagesPacket
import au.com.codeka.warworlds.common.proto.ChatRoom
import au.com.codeka.warworlds.common.proto.Packet
import com.google.common.collect.Lists

/**
 * Manages the chat rooms we're in and so on.
 */
class ChatManager private constructor() {
  fun create() {
    // TODO?
  }

  /**
   * Get the time of the most recent chat we have stored. When we connect to the server, we'll send
   * this value over so that it can update us on any chats that have happened since then.
   *
   *
   * So that we're not overwhelmed, we'll never return a value that's more than three days old.
   * If our most recent chat was more than three days ago, we'll need to clear out our cache and
   * rebuild it on-demand as you scroll back.
   */
  val lastChatTime: Long
    get() {
      val threeDaysAgo = System.currentTimeMillis() - 3 * Time.DAY
      val lastChatTime = App.dataStore.chat().lastChatTime
      if (lastChatTime == 0L) {
        return threeDaysAgo
      }
      if (lastChatTime < threeDaysAgo) {
        App.dataStore.chat().removeHistory()
        return threeDaysAgo
      }
      return lastChatTime
    }// TODO: get all the rooms, not just the global one.

  /** Get the list of rooms that we're in.  */
  val rooms: List<ChatRoom>
    get() =// TODO: get all the rooms, not just the global one.
      Lists.newArrayList(ChatRoom(id = null, name = "Global"))

  /** Gets count messages starting from startTime and going back in time.  */
  fun getMessages(room: ChatRoom, startTime: Long, count: Int): List<ChatMessage> {
    // TODO: if we don't have any, ask some from the server.
    return App.dataStore.chat().getMessages(room.id, startTime, count)
  }

  /** Gets all messages newer than time.  */
  fun getMessagesAfter(room: ChatRoom, time: Long): List<ChatMessage> {
    // TODO: if we don't have any, ask some from the server.
    return App.dataStore.chat().getMessagesAfter(room.id, time)
  }

  /** Gets all messages, regardless of room, from the given start time.  */
  fun getMessages(startTime: Long, count: Int): List<ChatMessage> {
    return App.dataStore.chat().getMessages(startTime, count)
  }

  /** Send the given [ChatMessage] to the server.  */
  fun sendMessage(msg: ChatMessage) {
    App.server.sendAsync(Packet(chat_msgs = ChatMessagesPacket(messages = Lists.newArrayList(msg))))
  }

  private val eventListener: Any = object : Any() {
    @EventHandler
    fun onChatMessagesPacket(pkt: ChatMessagesPacket) {
      App.dataStore.chat().addMessages(pkt.messages)
      App.eventBus.publish(ChatMessagesUpdatedEvent())
    }
  }

  companion object {
    @JvmField
    val i = ChatManager()
  }

  init {
    App.eventBus.register(eventListener)
  }
}